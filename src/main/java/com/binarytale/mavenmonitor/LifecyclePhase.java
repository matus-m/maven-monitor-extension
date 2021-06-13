package com.binarytale.mavenmonitor;

/**
 * Maven lifecycle phases ordered by reactor execution order.
 */
public enum LifecyclePhase
{
    PRE_CLEAN( "pre-clean" ),
    CLEAN( "clean" ),
    POST_CLEAN( "post-clean" ),
    VALIDATE( "validate" ),
    INITIALIZE( "initialize" ),
    GENERATE_SOURCES( "generate-sources" ),
    PROCESS_SOURCES( "process-sources" ),
    GENERATE_RESOURCES( "generate-resources" ),
    PROCESS_RESOURCES( "process-resources" ),
    COMPILE( "compile" ),
    PROCESS_CLASSES( "process-classes" ),
    GENERATE_TEST_SOURCES( "generate-test-sources" ),
    PROCESS_TEST_SOURCES( "process-test-sources" ),
    GENERATE_TEST_RESOURCES( "generate-test-resources" ),
    PROCESS_TEST_RESOURCES( "process-test-resources" ),
    TEST_COMPILE( "test-compile" ),
    PROCESS_TEST_CLASSES( "process-test-classes" ),
    TEST( "test" ),
    PREPARE_PACKAGE( "prepare-package" ),
    PACKAGE( "package" ),
    PRE_INTEGRATION_TEST( "pre-integration-test" ),
    INTEGRATION_TEST( "integration-test" ),
    POST_INTEGRATION_TEST( "post-integration-test" ),
    VERIFY( "verify" ),
    INSTALL( "install" ),
    DEPLOY( "deploy" ),
    PRE_SITE( "pre-site" ),
    SITE( "site" ),
    POST_SITE( "post-site" ),
    SITE_DEPLOY( "site-deploy" ),

    NONE( "" );

    private final String id;

    LifecyclePhase( String id )
    {
        this.id = id;
    }

    public String id()
    {
        return this.id;
    }

}