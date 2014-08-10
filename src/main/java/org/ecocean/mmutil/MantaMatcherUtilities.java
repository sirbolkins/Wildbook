/*
 * The Shepherd Project - A Mark-Recapture Framework
 * Copyright (C) 2011 Jason Holmberg
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.ecocean.mmutil;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jdo.Extent;
import javax.jdo.Query;
import org.ecocean.Encounter;
import org.ecocean.Shepherd;
import org.ecocean.SinglePhotoVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Giles Winstanley
 */
public final class MantaMatcherUtilities {
  /** SLF4J logger instance for writing log entries. */
  private static final Logger log = LoggerFactory.getLogger(MantaMatcherUtilities.class);

  private MantaMatcherUtilities() {}

  /**
   * Converts the specified file to a map of files, keyed by string for easy
   * referencing, comprising the following elements:
   * <ul>
   * <li>Original file (key: O).</li>
   * <li>File representing MantaMatcher &quot;candidate region&quot; photo (key: CR).</li>
   * <li>File representing MantaMatcher enhanced photo (key: EH).</li>
   * <li>File representing MantaMatcher feature photo (key: FT).</li>
   * <li>File representing MantaMatcher feature file (key: FEAT).</li>
   * <li>File representing MantaMatcher algorithm input file (key: MMA-INPUT).</li>
   * <li>File representing MantaMatcher algorithm regional input file (key: MMA-INPUT-REGIONAL).</li>
   * <li>File representing MantaMatcher output TXT file (key: TXT).</li>
   * <li>File representing MantaMatcher output CSV file (key: CSV).</li>
   * <li>File representing MantaMatcher regional output TXT file (key: TXT-REGIONAL).</li>
   * <li>File representing MantaMatcher regional output CSV file (key: CSV-REGIONAL).</li>
   * </ul>
   * All files are assumed to be in the same folder, and no checking is
   * performed to see if they exist.
   * @param spv {@code SinglePhotoVideo} instance denoting base reference image
   * @return Map of string to file for each MantaMatcher algorithm feature.
   */
  public static Map<String, File> getMatcherFilesMap(SinglePhotoVideo spv) {
    if (spv == null)
      throw new NullPointerException("Invalid file specified: null");
    Map<String, File> map = getMatcherFilesMap(spv.getFile());
    // MMA input files.
    map.put("MMA-INPUT", new File(spv.getFile().getParentFile(), spv.getDataCollectionEventID() + "_mmaInput.txt"));
    map.put("MMA-INPUT-REGIONAL", new File(spv.getFile().getParentFile(), spv.getDataCollectionEventID() + "_mmaInputRegional.txt"));
    // MMA results files: global.
    map.put("TXT", new File(spv.getFile().getParentFile(), spv.getDataCollectionEventID() + "_mmaOutput.txt"));
    map.put("CSV", new File(spv.getFile().getParentFile(), spv.getDataCollectionEventID() + "_mmaOutput.csv"));
    // MMA results files: regional.
    map.put("TXT-REGIONAL", new File(spv.getFile().getParentFile(), spv.getDataCollectionEventID() + "_mmaOutputRegional.txt"));
    map.put("CSV-REGIONAL", new File(spv.getFile().getParentFile(), spv.getDataCollectionEventID() + "_mmaOutputRegional.csv"));
    return map;
  }

  /**
   * Converts the specified file to a map of files, keyed by string for easy
   * referencing, comprising the following elements:
   * <ul>
   * <li>Original file (key: O).</li>
   * <li>File representing MantaMatcher &quot;candidate region&quot; photo (key: CR).</li>
   * <li>File representing MantaMatcher enhanced photo (key: EH).</li>
   * <li>File representing MantaMatcher feature photo (key: FT).</li>
   * <li>File representing MantaMatcher feature file (key: FEAT).</li>
   * </ul>
   * All files are assumed to be in the same folder, and no checking is
   * performed to see if they exist.
   * The functionality is centralized here to reduce naming errors/conflicts.
   * @param f base image file from which to reference other algorithm files
   * @return Map of string to file for each MantaMatcher algorithm feature.
   */
  public static Map<String, File> getMatcherFilesMap(File f) {
    if (f == null)
      throw new NullPointerException("Invalid file specified: null");
    String name = f.getName();
    String regFormat = MediaUtilities.REGEX_SUFFIX_FOR_WEB_IMAGES;
    if (!name.matches("^.+\\." + regFormat))
      throw new IllegalArgumentException("Invalid file type specified");
    String regex = "\\." + regFormat;
    File pf = f.getParentFile();
    File cr = new File(pf, name.replaceFirst(regex, "_CR.$1"));
    File eh = new File(pf, name.replaceFirst(regex, "_EH.$1"));
    File ft = new File(pf, name.replaceFirst(regex, "_FT.$1"));
    File feat = new File(pf, name.replaceFirst(regex, ".FEAT"));
    
    Map<String, File> map = new HashMap<String, File>(7);
    map.put("O", f);
    map.put("CR", cr);
    map.put("EH", eh);
    map.put("FT", ft);
    map.put("FEAT", feat);
    return map;
  }

  /**
   * Checks whether the MantaMatcher algorithm files exist for the specified
   * base file (only checks the files required for running {@code mmatch}).
   * @param f base image file from which to reference other algorithm files
   * @return true if all MantaMatcher files exist (O/CR/EH/FT/FEAT), false otherwise
   */
  public static boolean checkMatcherFilesExist(File f) {
    Map<String, File> mmFiles = getMatcherFilesMap(f);
    return mmFiles.get("O").exists() &&
            mmFiles.get("CR").exists() &&
            mmFiles.get("EH").exists() &&
            mmFiles.get("FT").exists() &&
            mmFiles.get("FEAT").exists();
  }

	/**
   * Creates a FreeMarker template configuration instance.
   * @param dir folder in which templates are located
   * @return Configuration instance for loading FreeMarker templates
   * @throws IOException
   */
  public static Configuration configureTemplateEngine(File dir) throws IOException
	{
		Configuration conf = new Configuration();
		conf.setDirectoryForTemplateLoading(dir);
		conf.setObjectWrapper(new DefaultObjectWrapper());
		conf.setDefaultEncoding("UTF-8");
    conf.setURLEscapingCharset("ISO-8859-1");
		conf.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);
		conf.setIncompatibleImprovements(new Version(2, 3, 20));
    return conf;
	}

  /**
   * Parses the MantaMatcher results text file for the specified SinglePhotoVideo.
   * @param conf FreeMarker configuration
   * @param mmaResultsFile MantaMatcher algorithm results text file
   * @param spv {@code SinglePhotoVideo} instance for base reference image
   * @param urlPrefixImage URL prefix for encounter folder (for image links)
   * @param pageUrlFormat Format string for encounter page URL (with <em>%s</em> placeholder)
   * @return A map containing parsed results ready for use with a FreeMarker template
   * @throws IOException
   * @throws ParseException
   * @throws TemplateException
   */
  @SuppressWarnings("unchecked")
  public static String getResultsHtml(Configuration conf, File mmaResultsFile, SinglePhotoVideo spv, String urlPrefixImage, String pageUrlFormat) throws IOException, ParseException, TemplateException {
    // Load results file.
    String text = new String(FileUtilities.loadFile(mmaResultsFile));
    // Convert to HTML results page.
    return MMAResultsProcessor.convertResultsToHtml(conf, text, spv, urlPrefixImage, pageUrlFormat);
  }

  /**
   * Collates text input for the MantaMatcher algorithm.
   * The output of this method is suitable for placing in a temporary file
   * to be access by the {@code mmatch} process.
   * @param shep {@code Shepherd} instance
   * @param encDir &quot;encounters&quot; directory
   * @param enc {@code Encounter} instance
   * @param spv {@code SinglePhotoVideo} instance
   * @return text suitable for MantaMatcher algorithm input file
   */
  @SuppressWarnings("unchecked")
  public static String collateAlgorithmInput(Shepherd shep, File encDir, Encounter enc, SinglePhotoVideo spv) {
    // Validate input.
    if (enc.getLocationID() == null)
      throw new IllegalArgumentException("Invalid location ID specified");
    if (encDir == null || !encDir.isDirectory())
      throw new IllegalArgumentException("Invalid encounter directory specified");
    if (enc == null || spv == null)
      throw new IllegalArgumentException("Invalid encounter/SPV specified");

    // Build query filter based on encounter.
    StringBuilder sbf = new StringBuilder();
    if (enc.getSpecificEpithet()!= null) {
      sbf.append("(this.specificEpithet == 'NULL'");
      sbf.append(" || this.specificEpithet == '").append(enc.getSpecificEpithet()).append("'");
      sbf.append(")");
    }
    if (enc.getPatterningCode() != null) {
      if (sbf.length() > 0)
        sbf.append(" && ");
      sbf.append("(this.patterningCode == 'NULL'");
      sbf.append(" || this.patterningCode == '").append(enc.getPatterningCode()).append("'");
      sbf.append(")");
    }
    if (enc.getSex() != null) {
      if (sbf.length() > 0)
        sbf.append(" && ");
      sbf.append("(this.sex == 'NULL'");
      sbf.append(" || this.sex == 'unknown'");
      sbf.append(" || this.sex == '").append(enc.getSex()).append("'");
      sbf.append(")");
    }
//    log.trace(String.format("Filter: %s", sbf.toString()));

    // Issue query.
    Extent ext = shep.getPM().getExtent(Encounter.class, true);
		Query query = shep.getPM().newQuery(ext);
    query.setFilter(sbf.toString());
    List<Encounter> list = (List<Encounter>)query.execute();

    // Collate results.
    StringBuilder sb = new StringBuilder();
//    sb.append(spv.getFile().getParent()).append("\n\n");
    sb.append(spv.getFile().getAbsolutePath()).append("\n\n");
    for (Encounter x : list) {
      if (!enc.getEncounterNumber().equals(x.getEncounterNumber()))
        sb.append(encDir.getAbsolutePath()).append(File.separatorChar).append(x.getEncounterNumber()).append("\n");
    }

    // Clean resources.
    query.closeAll();
    ext.closeAll();

    return sb.toString();
  }

  /**
   * Collates text input for the MantaMatcher algorithm.
   * The output of this method is suitable for placing in a temporary file
   * to be access by the {@code mmatch} process.
   * @param shep {@code Shepherd} instance
   * @param encDir &quot;encounters&quot; directory
   * @param enc {@code Encounter} instance
   * @param spv {@code SinglePhotoVideo} instance
   * @return text suitable for MantaMatcher algorithm input file
   */
  @SuppressWarnings("unchecked")
  public static String collateAlgorithmInputRegional(Shepherd shep, File encDir, Encounter enc, SinglePhotoVideo spv) {
    // Validate input.
    if (enc.getLocationID() == null)
      throw new IllegalArgumentException("Invalid location ID specified");
    if (encDir == null || !encDir.isDirectory())
      throw new IllegalArgumentException("Invalid encounter directory specified");
    if (enc == null || spv == null)
      throw new IllegalArgumentException("Invalid encounter/SPV specified");

    // Build query filter based on encounter.
    StringBuilder sbf = new StringBuilder();
    sbf.append("this.locationID == '").append(enc.getLocationID()).append("'");
    if (enc.getSpecificEpithet()!= null) {
      if (sbf.length() > 0)
        sbf.append(" && ");
      sbf.append("(this.specificEpithet == 'NULL'");
      sbf.append(" || this.specificEpithet == '").append(enc.getSpecificEpithet()).append("'");
      sbf.append(")");
    }
    if (enc.getPatterningCode() != null) {
      if (sbf.length() > 0)
        sbf.append(" && ");
      sbf.append("(this.patterningCode == 'NULL'");
      sbf.append(" || this.patterningCode == '").append(enc.getPatterningCode()).append("'");
      sbf.append(")");
    }
    if (enc.getSex() != null) {
      if (sbf.length() > 0)
        sbf.append(" && ");
      sbf.append("(this.sex == 'NULL'");
      sbf.append(" || this.sex == 'unknown'");
      sbf.append(" || this.sex == '").append(enc.getSex()).append("'");
      sbf.append(")");
    }
//    log.trace(String.format("Filter: %s", sbf.toString()));

    // Issue query.
    Extent ext = shep.getPM().getExtent(Encounter.class, true);
		Query query = shep.getPM().newQuery(ext);
    query.setFilter(sbf.toString());
    List<Encounter> list = (List<Encounter>)query.execute();

    // Collate results.
    StringBuilder sb = new StringBuilder();
//    sb.append(spv.getFile().getParent()).append("\n\n");
    sb.append(spv.getFile().getAbsolutePath()).append("\n\n");
    for (Encounter x : list) {
      if (!enc.getEncounterNumber().equals(x.getEncounterNumber()))
        sb.append(encDir.getAbsolutePath()).append(File.separatorChar).append(x.getEncounterNumber()).append("\n");
    }

    // Clean resources.
    query.closeAll();
    ext.closeAll();

    return sb.toString();
  }
}