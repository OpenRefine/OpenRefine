/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.openrefine.model;

import java.util.List;

import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.Row;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconJob;
import org.openrefine.model.recon.ReconStats;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ColumnMetadataTests {
	
	protected static class MyReconConfig extends ReconConfig {

		@Override
		public int getBatchSize() {
			return 40;
		}

		@Override
		public String getBriefDescription(String columnName) {
			return "My description";
		}

		@Override
		public ReconJob createJob(ColumnModel columnModel, long rowIndex, Row row, String columnName, Cell cell) {
			return null;
		}

		@Override
		public List<Recon> batchRecon(List<ReconJob> jobs, long historyEntryID) {
			return null;
		}

		@Override
		public Recon createNewRecon(long historyEntryID) {
			return null;
		}

		@Override
		public String getMode() {
			return "my-recon";
		}
		
	}
	
	ReconConfig reconConfig = new MyReconConfig();
	ColumnMetadata SUT = new ColumnMetadata("name", "organization_name", reconConfig, ReconStats.create(1L, 2L, 3L));
	
    @Test
    public void serializeColumn() throws Exception {
        ReconConfig.registerReconConfig("core", "my-recon", MyReconConfig.class);
        String json = "{\n"
                + "\"originalName\":\"name\","
                + "\"name\":\"organization_name\","
                + "\"reconConfig\":{"
                + "   \"mode\":\"my-recon\","
                + "    \"batchSize\":40"
                + "    },"
                + "\"reconStats\":{"
                + "    \"nonBlanks\":299,"
                + "    \"newTopics\":0,"
                + "    \"matchedTopics\":222"
                + "}}";
        TestUtils.isSerializedTo(ColumnMetadata.load(json), json, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void testMerge() {
        ColumnMetadata column2 = new ColumnMetadata("name2", "organization_name2", reconConfig, ReconStats.create(3L, 4L, 5L));
        ColumnMetadata expected = new ColumnMetadata("name", "organization_name", reconConfig, ReconStats.create(4L, 6L, 8L));
        
        Assert.assertEquals(SUT.merge(column2), expected);
    }
    
    @Test
    public void testEquals() {
        Assert.assertNotEquals(SUT, 4L);
        Assert.assertNotEquals(SUT, new ColumnMetadata("name", "organization_name", null, ReconStats.create(1L, 2L, 3L)));
        Assert.assertNotEquals(SUT, new ColumnMetadata("name", "organization_name", reconConfig, null));
        Assert.assertNotEquals(SUT, new ColumnMetadata("name2", "organization_name", reconConfig, ReconStats.create(1L, 2L, 3L)));
        Assert.assertEquals(SUT, new ColumnMetadata("name", "organization_name", reconConfig, ReconStats.create(1L, 2L, 3L)));
    }
}
