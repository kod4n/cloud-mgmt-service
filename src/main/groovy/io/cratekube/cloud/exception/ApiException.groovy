package io.cratekube.cloud.exception

import groovy.transform.InheritConstructors

@InheritConstructors
class ApiException extends RuntimeException {
  int errorCode = 500
}

@InheritConstructors class NotAcceptableException extends ApiException { int errorCode = 400 }
@InheritConstructors class NotFoundException extends ApiException { int errorCode = 404 }
