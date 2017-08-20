package io.vercy.brick.shop

import spock.lang.Specification

class BrickShopMainTest extends Specification {
    def "should create a new instance"() {
        expect:
            new BrickShopMain() != null
    }
}
