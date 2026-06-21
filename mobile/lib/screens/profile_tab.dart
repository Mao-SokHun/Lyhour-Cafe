import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../config/app_config.dart';
import '../services/app_state.dart';

class ProfileTab extends StatelessWidget {
  const ProfileTab({super.key});

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppState>();
    final home = app.homeData;

    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          ListTile(
            leading: const CircleAvatar(child: Icon(Icons.person)),
            title: Text(app.session.username ?? ''),
            subtitle: Text('Tier: ${home?['membershipTier'] ?? '—'}'),
          ),
          Card(
            child: ListTile(
              title: const Text('Loyalty points'),
              trailing: Text('${home?['loyaltyPoints'] ?? 0}'),
            ),
          ),
          Card(
            child: ListTile(
              title: const Text('Orders'),
              trailing: Text('${home?['orderCount'] ?? 0}'),
            ),
          ),
          const SizedBox(height: 8),
          Text('API: ${AppConfig.apiBaseUrl}', style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: 16),
          OutlinedButton.icon(
            onPressed: () async {
              // After Firebase setup, replace with FCM token from firebase_messaging
              final demoToken = 'mobile-demo-${app.session.username}';
              try {
                await app.registerPushToken(demoToken);
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Push token registered (use FCM token in production)')),
                  );
                }
              } catch (e) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
                }
              }
            },
            icon: const Icon(Icons.notifications_active_outlined),
            label: const Text('Register push token'),
          ),
          const SizedBox(height: 8),
          FilledButton(
            onPressed: () => app.logout(),
            child: const Text('Sign out'),
          ),
        ],
      ),
    );
  }
}
