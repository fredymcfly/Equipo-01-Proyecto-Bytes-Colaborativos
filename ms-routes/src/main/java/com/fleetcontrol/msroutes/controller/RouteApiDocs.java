package com.fleetcontrol.msroutes.controller;

/** Texts repeated in the Swagger annotations of {@link RouteApi}. */
final class RouteApiDocs {
  private RouteApiDocs() {}

  static final String REQUIRES_MANAGER = "Requires the MANAGER or ADMIN role.";
  static final String CODE_400 = "400";
  static final String CODE_401 = "401";
  static final String CODE_403 = "403";
  static final String CODE_404 = "404";
  static final String CODE_409 = "409";
  static final String CODE_503 = "503";
  static final String CODE_200 = "200";
  static final String DESC_UNAUTHORIZED = "UNAUTHORIZED or TOKEN_EXPIRED";
  static final String EXAMPLE_MISSING_TOKEN = "Missing token";
  static final String EXAMPLE_EXPIRED_TOKEN = "Expired token";
  static final String DESC_FORBIDDEN = "FORBIDDEN: the role has no permission for this operation";
  static final String EXAMPLE_FORBIDDEN = "Forbidden";
  static final String DESC_ROUTE_NOT_FOUND_CODE = "ROUTE_NOT_FOUND";
  static final String EXAMPLE_ROUTE_NOT_FOUND = "Route not found";
  static final String PARAM_ROUTE_ID = "Route id";
  static final String ROUTE_ID_EXAMPLE = "c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201";
}
