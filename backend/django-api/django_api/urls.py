from django.contrib import admin
from django.urls import path, include
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView
from rest_framework import permissions
from drf_yasg.views import get_schema_view
from drf_yasg import openapi



schema_view = get_schema_view(
   openapi.Info(
      title="Django API for lost/found system",
      default_version='v1',
      description="An API for a lost/found system",
      contact=openapi.Contact(email="hazratimohamamdmatin@gmail.com"),
   ),
   public=True,
   permission_classes=(permissions.AllowAny,),
)

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/users/', include('users.urls')),
    path('api/chats/', include('chats.urls')),
    path('api/swagger/', schema_view.with_ui('swagger', cache_timeout=0), name='swagger-schema'),
    path('api/redoc/', schema_view.with_ui('redoc', cache_timeout=0), name='redoc-schema'),
]
