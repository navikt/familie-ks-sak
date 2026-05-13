package no.nav.familie.ks.sak.kjerne

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class FaviconController {
    @GetMapping("favicon.ico")
    fun dummyFavicon() {
        // NOP denne skjuler No static resource favicon.ico found error log
    }
}
