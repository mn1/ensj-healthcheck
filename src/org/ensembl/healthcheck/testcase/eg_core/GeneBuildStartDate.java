package org.ensembl.healthcheck.testcase.eg_core;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.util.CollectionUtils;
import org.ensembl.healthcheck.util.SqlTemplate;

/**
 * Standalone test for checking genebuild.start_date meta key is of the form 2011-07-Ensembl
 * @author dstaines
 *
 */
public class GeneBuildStartDate extends AbstractEgCoreTestCase {

	public GeneBuildStartDate() {
		super();
	}

	protected final static String GET_START_DATE = "select meta_value from meta where meta_key='genebuild.start_date' and species_id=?";
	private static final String START_DATE_RE = "[0-9]{4}-[0-9]{2}-\\S+";
	protected final Pattern START_DATE_PATTERN = Pattern.compile(START_DATE_RE);

	@Override
	protected boolean runTest(DatabaseRegistryEntry dbre) {
		SqlTemplate templ = getTemplate(dbre);
		boolean result = true;
		for (int id : dbre.getSpeciesIds()) {
			List<String> gbs = templ.queryForDefaultObjectList(GET_START_DATE,
					String.class, id);
			if (gbs.size() != 1) {
				ReportManager.problem(this, dbre.getConnection(),
						"Expected one genebuild.start_date key only for species with ID "
								+ id + ": " + gbs.size() + " found instead");
				result = false;
			} else {
				String gb = CollectionUtils.getFirstElement(gbs, null);
				if (StringUtils.isEmpty(gb)) {
					ReportManager.problem(this, dbre.getConnection(),
							"Null or blank genebuild.start_date key for species "
									+ id);
					result = false;
				} else {
					if (!START_DATE_PATTERN.matcher(gb).matches()) {
						ReportManager
								.problem(
										this,
										dbre.getConnection(),
										"Start date "
												+ gb
												+ " for species "
												+ id
												+ " does not match the expected pattern "
												+ START_DATE_RE);
						result = false;
					}
				}
			}
		}
		return result;
	}

}