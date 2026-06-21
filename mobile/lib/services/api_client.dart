import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

import '../config/app_config.dart';

class Session {
  Session(this._storage);

  final FlutterSecureStorage _storage;
  static const _tokenKey = 'jwt_token';
  static const _usernameKey = 'username';

  String? token;
  String? username;

  Future<void> load() async {
    token = await _storage.read(key: _tokenKey);
    username = await _storage.read(key: _usernameKey);
  }

  Future<void> save(String jwt, String user) async {
    token = jwt;
    username = user;
    await _storage.write(key: _tokenKey, value: jwt);
    await _storage.write(key: _usernameKey, value: user);
  }

  Future<void> clear() async {
    token = null;
    username = null;
    await _storage.delete(key: _tokenKey);
    await _storage.delete(key: _usernameKey);
  }

  bool get isLoggedIn => token != null && token!.isNotEmpty;

  Map<String, String> authHeaders() => {
        'Authorization': 'Bearer $token',
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      };
}

class ApiClient {
  ApiClient(this.session);

  final Session session;

  Future<Map<String, dynamic>> getJson(String path, {bool auth = true}) async {
    final headers = auth && session.isLoggedIn
        ? session.authHeaders()
        : {'Accept': 'application/json'};
    final response = await http.get(AppConfig.uri(path), headers: headers);
    if (response.statusCode >= 400) {
      throw ApiException(response.statusCode, response.body);
    }
    return jsonDecode(response.body) as Map<String, dynamic>;
  }

  Future<List<dynamic>> getJsonList(String path, {bool auth = true}) async {
    final headers = auth && session.isLoggedIn
        ? session.authHeaders()
        : {'Accept': 'application/json'};
    final response = await http.get(AppConfig.uri(path), headers: headers);
    if (response.statusCode >= 400) {
      throw ApiException(response.statusCode, response.body);
    }
    return jsonDecode(response.body) as List<dynamic>;
  }

  Future<Map<String, dynamic>> postJson(
    String path,
    Map<String, dynamic> body, {
    bool auth = true,
  }) async {
    final headers = auth && session.isLoggedIn
        ? session.authHeaders()
        : {'Content-Type': 'application/json', 'Accept': 'application/json'};
    final response = await http.post(
      AppConfig.uri(path),
      headers: headers,
      body: jsonEncode(body),
    );
    if (response.statusCode >= 400) {
      throw ApiException(response.statusCode, response.body);
    }
    if (response.body.isEmpty) return {};
    return jsonDecode(response.body) as Map<String, dynamic>;
  }

  Future<void> postPushToken(String token) async {
    final response = await http.post(
      AppConfig.uri('/api/v1/mobile/push-token', {'token': token}),
      headers: session.authHeaders(),
    );
    if (response.statusCode >= 400) {
      throw ApiException(response.statusCode, response.body);
    }
  }
}

class ApiException implements Exception {
  ApiException(this.statusCode, this.body);
  final int statusCode;
  final String body;

  @override
  String toString() => 'API $statusCode: $body';
}
