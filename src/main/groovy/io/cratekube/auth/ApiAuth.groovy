package io.cratekube.auth

import groovy.transform.AnnotationCollector
import io.dropwizard.auth.Auth
import io.swagger.annotations.ApiParam

@Auth
@ApiParam(hidden = true)
@AnnotationCollector
@interface ApiAuth {}
