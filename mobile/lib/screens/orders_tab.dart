import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/app_state.dart';

class OrdersTab extends StatefulWidget {
  const OrdersTab({super.key});

  @override
  State<OrdersTab> createState() => _OrdersTabState();
}

class _OrdersTabState extends State<OrdersTab> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppState>().refreshOrders();
    });
  }

  @override
  Widget build(BuildContext context) {
    final orders = context.watch<AppState>().orders;

    return Scaffold(
      appBar: AppBar(
        title: const Text('My orders'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => context.read<AppState>().refreshOrders(),
          ),
        ],
      ),
      body: orders.isEmpty
          ? const Center(child: Text('No orders yet'))
          : ListView.separated(
              itemCount: orders.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (_, i) {
                final o = orders[i];
                return ListTile(
                  title: Text('Order #${o['id']}'),
                  subtitle: Text('${o['status']} · \$${o['totalPrice']}'),
                  trailing: Text(o['orderDate']?.toString().substring(0, 10) ?? ''),
                );
              },
            ),
    );
  }
}
