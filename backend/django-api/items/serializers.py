import base64
import binascii

from rest_framework import serializers

from users.serializers import UserPublicSerializer
from .models import Item, ItemCategory


class ItemSerializer(serializers.ModelSerializer):
    class Base64BinaryField(serializers.Field):
        def __init__(self, *args, max_size_bytes=None, **kwargs):
            super().__init__(*args, **kwargs)
            self.max_size_bytes = max_size_bytes

        def to_internal_value(self, data):
            if data in (None, ''):
                return None

            if not isinstance(data, str):
                raise serializers.ValidationError('Image must be a base64-encoded string.')

            raw_data = data.split(',', 1)[1] if ',' in data else data
            try:
                decoded = base64.b64decode(raw_data, validate=True)
            except (binascii.Error, ValueError):
                raise serializers.ValidationError('Invalid base64 image data.')

            if self.max_size_bytes is not None and len(decoded) > self.max_size_bytes:
                raise serializers.ValidationError(
                    f'Image exceeds max size of {self.max_size_bytes} bytes.'
                )

            return decoded

        def to_representation(self, value):
            if value is None:
                return None
            return base64.b64encode(value).decode('ascii')

    reporter = UserPublicSerializer(read_only=True)
    category_name = serializers.CharField(source='category.name', read_only=True)
    image = Base64BinaryField(required=False, allow_null=True, max_size_bytes=1024 * 1024)

    class Meta:
        model = Item
        fields = ['id', 'type', 'name', 'description', 'category', 'category_name', 'latitude',
            'longitude', 'status', 'image', 'reporter', 'created_at', 'updated_at']
        read_only_fields = ['id', 'reporter', 'created_at', 'updated_at', 'status']

    def validate_latitude(self, value):
        if value is not None and (value < 35.700310 or value > 35.706914):
            raise serializers.ValidationError("Latitude must be between 35.700310 and 35.706914 degrees.")
        return value

    def validate_longitude(self, value):
        if value is not None and (value < 51.348554 or value > 51.353770):
            raise serializers.ValidationError("Longitude must be between 51.348554 and 51.353770 degrees.")
        return value

    def create(self, validated_data):
        # Check if category_name was provided in the request
        category_name = self.initial_data.get('category_name')
        
        if category_name:
            # Get or create the category by name
            category, created = ItemCategory.objects.get_or_create(
                name=category_name.strip()
            )
            validated_data['category'] = category
        
        return super().create(validated_data)

    def update(self, instance, validated_data):
        return super().update(instance, validated_data)

    def to_representation(self, instance):
        data = super().to_representation(instance)
        return data