import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/app_state.dart';

class MenuTab extends StatelessWidget {
  const MenuTab({super.key});

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    final menu = (app.homeData?['menu'] as List<dynamic>?) ?? [];

    return RefreshIndicator(
      onRefresh: app.refreshHome,
      child: CustomScrollView(
        slivers: [
          SliverAppBar.large(title: Text(app.homeData?['shop']?.toString() ?? 'Lyhour Coffee')),
          if (menu.isEmpty)
            const SliverFillRemaining(child: Center(child: Text('No items — pull to refresh')))
          else
            SliverList.separated(
              itemCount: menu.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (context, i) {
                final p = menu[i] as Map<String, dynamic>;
                final inStock = p['inStock'] as bool? ?? true;
                return ListTile(
                  title: Text(p['name']?.toString() ?? ''),
                  subtitle: Text('\$${p['price']} · ${p['category'] ?? ''}'),
                  trailing: inStock
                      ? IconButton(
                          icon: const Icon(Icons.add_circle_outline),
                          onPressed: () {
                            context.read<AppState>().addToCart(p);
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(content: Text('Added ${p['name']}')),
                            );
                          },
                        )
                      : const Text('Sold out', style: TextStyle(color: Colors.red)),
                );
              },
            ),
        ],
      ),
    );
  }
}
