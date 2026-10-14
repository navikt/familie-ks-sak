package no.nav.familie.ks.sak.common.domeneparser

import io.cucumber.datatable.DataTable

fun DataTable.tilRader(): List<Map<String, String?>> =
    asMaps().map { rad ->
        rad.mapKeys { (nøkkel, _) ->
            requireNotNull(nøkkel) {
                "Cucumber-tabellen mangler kolonnenavn"
            }
        }
    }
