import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/app_state.dart';

class CartTab extends StatelessWidget {
  const CartTab({super.key});

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    final total = app.cart.fold<double>(0, (s, c) => s + c.lineTotal);

    return Scaffold(
      appBar: AppBar(title: const Text('Cart')),
      body: app.cart.isEmpty
          ? const Center(child: Text('Your cart is empty'))
          : ListView.separated(
              itemCount: app.cart.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (_, i) {
                final line = app.cart[i];
                return ListTile(
                  title: Text(line.name),
                  subtitle: Text('\$${line.price.toStringAsFixed(2)} × ${line.quantity}'),
                  trailing: Text('\$${line.lineTotal.toStringAsFixed(2)}'),
                );
              },
            ),
      bottomNavigationBar: app.cart.isEmpty
          ? null
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: FilledButton(
                  onPressed: () async {
                    final err = await app.checkout();
                    if (!context.mounted) return;
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text(err ?? 'Order placed — pay at pickup')),
                    );
                  },
                  child: Text('Checkout · \$${total.toStringAsFixed(2)}'),
                ),
              ),
            ),
    );
  }
}
