package no.nav.familie.ks.sak.app.integrasjon.infotrygd.domene;

public class AktivKontantstøtteInfo {
    private Boolean harAktivKontantstotte;

    public AktivKontantstøtteInfo() {
    }

    public AktivKontantstøtteInfo(Boolean harAktivKontantstotte) {
        this.harAktivKontantstotte = harAktivKontantstotte;
    }

    public Boolean getHarAktivKontantstotte() {
        return harAktivKontantstotte;
    }

    public void setHarAktivKontantstotte(Boolean harAktivKontantstotte) {
        this.harAktivKontantstotte = harAktivKontantstotte;
    }
}