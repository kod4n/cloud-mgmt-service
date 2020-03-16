package io.cratekube.auth

import groovy.transform.Immutable

import java.security.Principal

/**
 * Default user principal to use for authentication.
 */
@Immutable
class User implements Principal {
  /**
   * Username for the operating user.
   */
  String name

  /**
   * Unique set of roles this user has been granted.
   */
  Set<String> roles = [] as Set<String>
}
