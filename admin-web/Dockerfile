FROM php:8.2-apache

# Enable Apache mod_rewrite
RUN a2enmod rewrite

# Install PostgreSQL PHP extensions
RUN apt-get update && apt-get install -y libpq-dev \
    && docker-php-ext-install pdo pdo_pgsql

# Copy the admin-web files into the Apache document root
COPY . /var/www/html/

# Expose port 80 (Render forwards traffic to the port EXPOSEd)
EXPOSE 80
