Ext.override(Sonatype.repoServer.VirtualRepositoryEditor, {
    afterProviderSelectHandler: function (combo, rec, index) {
        var provider = rec.data.provider;
        var sourceRepoCombo = this.form.findField("shadowOf");
        sourceRepoCombo.clearValue();
        sourceRepoCombo.focus();
        assert(provider);
        if (provider == "android-shadow") {
            sourceRepoCombo.store.filterBy(function fn(rec, id) {
                if (rec.data.repoType != "virtual" && rec.data.format == "maven2") {
                    return true
                }
                return false
            })
        } else {
            if (provider == "m1-m2-shadow") {
                sourceRepoCombo.store.filterBy(function fn(rec, id) {
                    if (rec.data.repoType != "virtual" && rec.data.format == "maven1") {
                        return true
                    }
                    return false
                })
            } else {
                if (provider == "m2-m1-shadow") {
                    sourceRepoCombo.store.filterBy(function fn(rec, id) {
                        if (rec.data.repoType != "virtual" && rec.data.format == "maven2") {
                            return true
                        }
                        return false
                    })
                }
            }
        }
    }
});