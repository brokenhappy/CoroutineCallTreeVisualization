package com.woutwerkman

/**
 * Functions with this annotation will not have call tracking
 * logic inserted by the call tracking agent.
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class NonTrackedJava
