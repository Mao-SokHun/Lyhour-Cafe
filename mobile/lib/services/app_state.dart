import 'package:flutter/foundation.dart';

import 'api_client.dart';

class AppState extends ChangeNotifier {
  AppState(this.api);

  final ApiClient api;
  Session get session => api.session;

  bool initialized = false;
  String? error;
  Map<String, dynamic>? homeData;
  List<Map<String, dynamic>> orders = [];
  final List<CartLineRef> cart = [];

  Future<void> init() async {
    await session.load();
    initialized = true;
    if (session.isLoggedIn) {
      await refreshHome();
    }
    notifyListeners();
  }

  Future<bool> login(String username, String password) async {
    error = null;
    try {
      final data = await api.postJson(
        '/api/v1/auth/login',
        {'username': username, 'password': password},
        auth: false,
      );
      await session.save(data['token'] as String, data['username'] as String);
      await refreshHome();
      notifyListeners();
      return true;
    } on ApiException catch (e) {
      error = e.statusCode == 401 ? 'Invalid username or password' : e.toString();
      notifyListeners();
      return false;
    }
  }

  Future<void> logout() async {
    await session.clear();
    homeData = null;
    orders = [];
    cart.clear();
    notifyListeners();
  }

  Future<void> refreshHome() async {
    homeData = await api.getJson('/api/v1/mobile/home');
    notifyListeners();
  }

  Future<void> refreshOrders() async {
    final list = await api.getJsonList('/api/v1/orders');
    orders = list.cast<Map<String, dynamic>>();
    notifyListeners();
  }

  void addToCart(Map<String, dynamic> productJson) {
    final id = (productJson['id'] as num).toInt();
    final existing = cart.where((c) => c.productId == id).firstOrNull;
    if (existing != null) {
      existing.quantity++;
    } else {
      cart.add(CartLineRef(
        productId: id,
        name: productJson['name'] as String? ?? '',
        price: (productJson['price'] as num?)?.toDouble() ?? 0,
        quantity: 1,
      ));
    }
    notifyListeners();
  }

  Future<String?> checkout() async {
    if (cart.isEmpty) return 'Cart is empty';
    try {
      await api.postJson('/api/v1/orders', {
        'items': cart
            .map((c) => {
                  'productId': c.productId,
                  'quantity': c.quantity,
                  'size': 'Medium',
                })
            .toList(),
        'paymentMethod': 'PAY_AT_PICKUP',
        'fulfillmentType': 'PICKUP',
      });
      cart.clear();
      await refreshOrders();
      notifyListeners();
      return null;
    } on ApiException catch (e) {
      return e.toString();
    }
  }

  Future<void> registerPushToken(String token) async {
    if (!session.isLoggedIn) return;
    await api.postPushToken(token);
  }
}

class CartLineRef {
  CartLineRef({
    required this.productId,
    required this.name,
    required this.price,
    required this.quantity,
  });

  final int productId;
  final String name;
  final double price;
  int quantity;

  double get lineTotal => price * quantity;
}

extension _FirstOrNull<E> on Iterable<E> {
  E? get firstOrNull {
    final it = iterator;
    if (!it.moveNext()) return null;
    return it.current;
  }
}
