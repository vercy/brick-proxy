package io.vercy.brick.proxy

import spock.lang.Specification

class BrickProxyMainTest extends Specification {
    def "should create a new instance"() {
        expect:
            new BrickProxyMain() != null
    }
}
