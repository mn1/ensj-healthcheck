/*
  Copyright (C) 2003 EBI, GRL
 
  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.
 
  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
 
  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.ensembl.healthcheck.testcase;

import java.sql.*;

import org.ensembl.healthcheck.*;

import org.ensembl.healthcheck.util.*;

/**
 * Check that feature co-ords make sense.
 */
public class CheckFeatureCoordsTestCase extends EnsTestCase {
  
  /**
   * Creates a new instance of CheckFeatureCoordsTestCase
   */
  public CheckFeatureCoordsTestCase() {
    addToGroup("post_genebuild");
    setDescription("Check that feature co-ords (DNA and protein) make sense.");
  }
  
  /** 
   * Iterate over each affected database and perform various checks.
   * @return Result.
   */
  public TestResult run() {
    
    boolean result = true;
    
    String[] featureTables = { "dna_align_feature", "protein_align_feature", "exon", "repeat_feature",
			       "prediction_transcript", "simple_feature", "marker_feature" };

    
    for( int tableIndex = 0; tableIndex < featureTables.length; tableIndex++ ) {
	String tableName = featureTables[tableIndex];

	DatabaseConnectionIterator it = getDatabaseConnectionIterator();
    
	while (it.hasNext()) {
      
	    Connection con = (Connection)it.next();
      
	    logger.info("Checking " + tableName + " for " + DBUtils.getShortDatabaseName(con) + " ...");

	    String sql = "SELECT f." + tableName + "_id, f.contig_start, f.contig_end, c.length " +
		"FROM " + tableName + " f, contig c " +
		"WHERE c.contig_id = f.contig_id " +
		"AND ( f.contig_start > f.contig_end " +
		" OR f.contig_start < 1 " +
		" OR f.contig_end > c.length )";
	    try {
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		int smallStart = 0;
		int swapCoords = 0;
		int largeEnd = 0;
		
		while( rs.next() ) {
		    int id = rs.getInt( 1 );
		    int start = rs.getInt( 2 );
		    int end = rs.getInt( 3 );
		    int len = rs.getInt( 4 );
		    
		    if( end < start && swapCoords < 10 ) { 
			ReportManager.warning(this, con, tableName + " " + id + " has contig_start > contig_end");
			swapCoords++;
		    }
		    
		    if( end > len && largeEnd < 10 ) { 
			ReportManager.warning(this, con, tableName + " " + id + " has contig_end > contig_length");
			largeEnd++;
		    }
		    
		    if( start < 1 && smallStart < 10 ) { 
			ReportManager.warning(this, con, tableName + " " + id + " has contig_start < 1" );
			smallStart++;
		    }
		}
		
		if( smallStart + largeEnd + swapCoords > 0 ) {
		    ReportManager.problem( this, con, ( tableName + " table has coordinate mistakes" ));
		    result = false;
		} else {
		    ReportManager.correct( this, con, ( tableName + " coordinates seem correct" ));
		}
	    } catch( SQLException e ) {
		e.printStackTrace();
	    }
	}
    }
    
    return new TestResult(getShortTestName(), result);
    
  } // run
  
} // CheckFeatureCoordsTestCase
