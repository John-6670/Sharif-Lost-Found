from django.http import Http404
from django.db import transaction
from django.db.models import Q

from rest_framework import status
from rest_framework.permissions import IsAuthenticated, BasePermission, SAFE_METHODS, AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Item, ItemReport, Comment, CommentReport
from .serializers import ItemSerializer, CommentSerializer

class IsReporterOrReadOnly(BasePermission):
	def has_object_permission(self, request, view, obj):
		if request.method in SAFE_METHODS:
			return True
		return obj.reporter_id == request.user.id


class ItemListCreateView(APIView):
	def get_permissions(self):
		if self.request.method == 'GET':
			return [AllowAny()]
		return [IsAuthenticated()]

	def get_queryset(self):
		queryset = Item.objects.select_related('reporter', 'category').all()

		params = self.request.query_params

		# Exact-match filters
		if 'type' in params:
			queryset = queryset.filter(type=params.get('type'))
		if 'status' in params:
			queryset = queryset.filter(status=params.get('status'))
		if 'category' in params:
			queryset = queryset.filter(category_id=params.get('category'))
		if 'category_name' in params:
			queryset = queryset.filter(category__name__iexact=params.get('category_name'))

		# Location range filters
		if 'lat_min' in params:
			try:
				queryset = queryset.filter(latitude__gte=float(params.get('lat_min')))
			except (ValueError, TypeError):
				pass
		if 'lat_max' in params:
			try:
				queryset = queryset.filter(latitude__lte=float(params.get('lat_max')))
			except (ValueError, TypeError):
				pass
		if 'lon_min' in params:
			try:
				queryset = queryset.filter(longitude__gte=float(params.get('lon_min')))
			except (ValueError, TypeError):
				pass
		if 'lon_max' in params:
			try:
				queryset = queryset.filter(longitude__lte=float(params.get('lon_max')))
			except (ValueError, TypeError):
				pass

		# Search in name and description
		if 'search' in params:
			search_term = params.get('search', '').strip()
			if search_term:
				queryset = queryset.filter(Q(name__icontains=search_term) | Q(description__icontains=search_term))

		# Generic field filter support: ?name=...&description=...
		for key, value in params.items():
			if key in ['type', 'status', 'category', 'category_name', 'page', 'ordering', 'search', 'lat_min', 'lat_max', 'lon_min', 'lon_max']:
				continue
			if value:
				queryset = queryset.filter(**{key: value})

		return queryset

	def get(self, request):
		items = self.get_queryset()
		serializer = ItemSerializer(items, many=True)
		return Response(serializer.data, status=status.HTTP_200_OK)

	def post(self, request):
		serializer = ItemSerializer(data=request.data)
		if not serializer.is_valid():
			return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

		serializer.save(reporter=request.user)
		return Response(serializer.data, status=status.HTTP_201_CREATED)


class ItemDetailView(APIView):
	def get_permissions(self):
		if self.request.method == 'GET':
			return [AllowAny()]
		return [IsAuthenticated(), IsReporterOrReadOnly()]

	def get_object(self, item_id):
		try:
			return Item.objects.select_related('reporter', 'category').get(id=item_id)
		except Item.DoesNotExist:
			raise Http404

	def get(self, request, item_id):
		item = self.get_object(item_id)
		self.check_object_permissions(request, item)
		serializer = ItemSerializer(item)
		return Response(serializer.data, status=status.HTTP_200_OK)

	def put(self, request, item_id):
		item = self.get_object(item_id)
		self.check_object_permissions(request, item)

		serializer = ItemSerializer(item, data=request.data)
		if serializer.is_valid():
			serializer.save()
			return Response(serializer.data, status=status.HTTP_200_OK)
		return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

	def patch(self, request, item_id):
		item = self.get_object(item_id)
		self.check_object_permissions(request, item)

		serializer = ItemSerializer(item, data=request.data, partial=True)
		if serializer.is_valid():
			serializer.save()
			return Response(serializer.data, status=status.HTTP_200_OK)
		return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

	def delete(self, request, item_id):
		item = self.get_object(item_id)
		self.check_object_permissions(request, item)
		item.delete()
		return Response(status=status.HTTP_204_NO_CONTENT)


class ReportItemView(APIView):
	permission_classes = [IsAuthenticated]

	def post(self, request, item_id):
		try:
			item = Item.objects.get(id=item_id)
		except Item.DoesNotExist:
			return Response({'error': 'Item not found'}, status=status.HTTP_404_NOT_FOUND)

		# Check if user already reported this item
		if ItemReport.objects.filter(item=item, reported_by=request.user).exists():
			return Response({'error': 'You have already reported this item'}, status=status.HTTP_400_BAD_REQUEST)

		# Create report and increment count atomically
		with transaction.atomic():
			ItemReport.objects.create(item=item, reported_by=request.user)
			item.reported_counts += 1
			item.save()

			# Check if item should be deleted
			if item.reported_counts >= 5:
				item.delete()
				return Response({'message': 'Item reported and removed due to multiple reports'}, status=status.HTTP_200_OK)

		return Response({'message': 'Item reported successfully', 'total_reports': item.reported_counts}, status=status.HTTP_200_OK)


class IsCommenterOrReadOnly(BasePermission):
    def has_object_permission(self, request, view, obj):
        if request.method in SAFE_METHODS:
            return True
        return obj.commenter_id == request.user.id


class CommentListCreateView(APIView):
    def get_permissions(self):
        if self.request.method == 'GET':
            return [AllowAny()]
        return [IsAuthenticated()]

    def get_queryset(self):
        queryset = Comment.objects.select_related('commenter', 'item').all()
        
        # Filter by item if provided
        item_id = self.request.query_params.get('item')
        if item_id:
            queryset = queryset.filter(item_id=item_id)
        
        return queryset.order_by('-created_at')

    def get(self, request):
        comments = self.get_queryset()
        serializer = CommentSerializer(comments, many=True)
        return Response(serializer.data, status=status.HTTP_200_OK)

    def post(self, request):
        serializer = CommentSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        serializer.save(commenter=request.user)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


class ReportCommentView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, comment_id):
        try:
            comment = Comment.objects.get(id=comment_id)
        except Comment.DoesNotExist:
            return Response({'error': 'Comment not found'}, status=status.HTTP_404_NOT_FOUND)

        # Check if user already reported this comment
        if CommentReport.objects.filter(comment=comment, reported_by=request.user).exists():
            return Response({'error': 'You have already reported this comment'}, status=status.HTTP_400_BAD_REQUEST)

        # Create report and increment count atomically
        with transaction.atomic():
            CommentReport.objects.create(comment=comment, reported_by=request.user)
            comment.reports_count += 1
            comment.save()

            # Check if comment should be deleted
            if comment.reports_count >= 5:
                comment.delete()
                return Response({'message': 'Comment reported and removed due to multiple reports'}, status=status.HTTP_200_OK)

        return Response({'message': 'Comment reported successfully', 'total_reports': comment.reports_count}, status=status.HTTP_200_OK)
