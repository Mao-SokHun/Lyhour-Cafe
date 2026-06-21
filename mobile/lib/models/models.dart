class Product {
  Product({
    required this.id,
    required this.name,
    required this.description,
    required this.price,
    required this.category,
    required this.inStock,
  });

  final int id;
  final String name;
  final String description;
  final double price;
  final String category;
  final bool inStock;

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String? ?? '',
      description: json['description'] as String? ?? '',
      price: (json['price'] as num?)?.toDouble() ?? 0,
      category: json['category'] as String? ?? '',
      inStock: json['inStock'] as bool? ?? true,
    );
  }
}

class CartLine {
  CartLine({required this.product, this.quantity = 1});

  final Product product;
  int quantity;

  double get lineTotal => product.price * quantity;
}

class OrderSummary {
  OrderSummary({
    required this.id,
    required this.totalPrice,
    required this.status,
    required this.orderDate,
  });

  final int id;
  final String totalPrice;
  final String status;
  final String orderDate;

  factory OrderSummary.fromJson(Map<String, dynamic> json) {
    return OrderSummary(
      id: (json['id'] as num).toInt(),
      totalPrice: json['totalPrice'].toString(),
      status: json['status'] as String? ?? '',
      orderDate: json['orderDate']?.toString() ?? '',
    );
  }
}
