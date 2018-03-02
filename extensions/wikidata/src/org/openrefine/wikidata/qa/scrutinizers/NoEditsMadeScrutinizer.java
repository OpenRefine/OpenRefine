package org.openrefine.wikidata.qa.scrutinizers;

import java.util.List;

import org.openrefine.wikidata.updates.ItemUpdate;


public class NoEditsMadeScrutinizer extends EditScrutinizer {
    
    public static final String type = "no-edit-generated";

    @Override
    public void scrutinize(List<ItemUpdate> edit) {
        if(edit.stream().allMatch(e -> e.isNull())) {
            info(type);
        }

    }

}
