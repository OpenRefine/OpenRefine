package org.openrefine.wikidata.qa.scrutinizers;

import java.util.List;

import org.openrefine.wikidata.schema.ItemUpdate;


public class NoEditsMadeScrutinizer extends EditScrutinizer {

    @Override
    public void scrutinize(List<ItemUpdate> edit) {
        if(edit.stream().allMatch(e -> e.isNull())) {
            info("no-edit-generated");
        }

    }

}
