package io.vercy.brick.site

import spock.lang.Specification

class BrickSiteMainTest extends Specification {
    def "should create a new instance"() {
        expect:
            new BrickSiteMain() != null
    }
}
