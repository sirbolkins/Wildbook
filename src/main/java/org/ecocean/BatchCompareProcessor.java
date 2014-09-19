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

package org.ecocean;

import java.io.*;
import java.util.*;

import org.ecocean.servlet.ServletUtilities;

import javax.servlet.ServletContext;

/**
 * Does actual comparison processing of batch-uploaded images.
 *
 * @author Jon Van Oast
 */
public final class BatchCompareProcessor implements Runnable {
  public static final String SESSION_KEY_PROCESS = "ImportCSVProcess";
  public static final String SESSION_KEY_COMPARE = "ImportCSVCompare";
  //private static Logger log = LoggerFactory.getLogger(BatchProcessor.class);

  /** ServletContext for web application, to allow access to resources. */
  private ServletContext servletContext;
  /** Username of person doing batch upload (for logging in comments). */

	private int countTotal = 0;
	private int countComplete = 0;

  /** Enumeration representing possible status values for the batch processor. */
  public enum Status { WAITING, INIT, RUNNING, FINISHED, ERROR };
  /** Current status of the batch processor. */
  private Status status = Status.WAITING;
  /** Throwable instance produced by the batch processor (if any). */
  private Throwable thrown;

	private String method = null;
	private List<String> args = null;
  private String context = "context0";
	private String batchID = null;

  public BatchCompareProcessor(ServletContext servletContext, String context, String method, List<String> args, String batchID) {
		this.servletContext = servletContext;
		this.context = context;
		this.args = args;
		this.method = method;
		this.batchID = batchID;
System.out.println("in BatchCompareProcessor()");
	}

	public int getCountTotal() {
		return this.countTotal;
	}
	public int getCountComplete() {
		return this.countComplete;
	}




//// this.args will contain encounter ids... this (pre)processes the images for those
	public void npmProcess() {
		String rootDir = servletContext.getRealPath("/");
		String baseDir = ServletUtilities.dataDir(context, rootDir);
System.out.println("start npmProcess()");

		this.countTotal = this.args.size();

		for (String eid : this.args) {
			String epath = Encounter.dir(baseDir, eid);
System.out.println(epath);
//~jon/npm_process -contr_thr 0.02 -sigma 1.2 /opt/tomcat7/webapps/cascadia_data_dir/encounterxs 0 0 4 1 2
			//String[] command = new String[]{"/usr/bin/npm_process", "-contr_thr", "0.02", "-sigma", "1.2", epath, "0", "0", "4", "1", "2"};
//home/jon/npm_process -contr_thr 0.02 -sigma 1.2 cascadia_data_dir/ 0 0 4 1 2
			//String[] command = new String[]{"sh", "/opt/tomcat7/bin/run_npm_process.sh", epath};
			String[] command = new String[]{"/usr/local/bin/npm_process_wrapper.sh", epath};

			ProcessBuilder pb = new ProcessBuilder();
			Map<String, String> env = pb.environment();
			env.put("LD_LIBRARY_PATH", "/usr/local/lib/opencv2.4.7");
			pb.command(command);
System.out.println("====================== npm_process on " + eid);

			try {
				Process proc = pb.start();
				BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				String line;
				while ((line = stdInput.readLine()) != null) {
					System.out.println(eid + ">>>> " + line);
				}
				proc.waitFor();
System.out.println(eid + " DONE?????");
				////int returnCode = p.exitValue();

			} catch (Exception ioe) {
				System.out.println("oops: " + ioe.toString());
			}

			this.countComplete++;

			try {
				PrintWriter statusOut = new PrintWriter(baseDir + "/encounters/importcsv.lock");
				statusOut.println(Integer.toString(this.countComplete) + " " + Integer.toString(this.countTotal));
				statusOut.close();
			} catch (Exception ex) {
				System.out.println("could not write " + baseDir + "/encounters/importcsv.lock: " + ex.toString());
			}
		}

		File ilock = new File(baseDir + "/encounters/importcsv.lock");
		if (ilock.exists()) ilock.delete();

System.out.println("RETURN");
	}


////// does the comparison/match, given a bunch of file paths as
	public void npmCompare() {
		String rootDir = servletContext.getRealPath("/");
		String baseDir = ServletUtilities.dataDir(context, rootDir);
		//String batchDir = baseDir + "/match_images/" + this.batchID;
System.out.println("start npmCompare()");

		this.countTotal = this.args.size();  //size of images uploaded

		for (String imgpath : this.args) {
			//String epath = Encounter.dir(baseDir, eid);
System.out.println(imgpath);
//~jon/npm_process -contr_thr 0.02 -sigma 1.2 /opt/tomcat7/webapps/cascadia_data_dir/encounterxs 0 0 4 1 2
//whalematch.exe -sscale 1.1 15.16 "C:\flukefolder" "C:\flukefolder\whale1\whale1fluke1.jpg"  0 0 2 0 -o whaleID_whale1fluke1.xhtml -c whaleID_whale1fluke1.csv
			String[] command = new String[]{"/usr/local/bin/npm_both_wrapper.sh", imgpath, baseDir + "/encounters"};
			//String[] command = new String[]{"/usr/bin/npm_match", "-sscale", "1.1", "15.16", baseDir + "/encounters", imgpath, "0", "0", "2", "0", "-o", "/tmp/out.txt", "-c", "/tmp/out.csv"};
//home/jon/npm_process -contr_thr 0.02 -sigma 1.2 cascadia_data_dir/ 0 0 4 1 2
			//String[] command = new String[]{"sh", "/opt/tomcat7/bin/run_npm_process.sh", epath};

			ProcessBuilder pb = new ProcessBuilder();
			Map<String, String> env = pb.environment();
			env.put("LD_LIBRARY_PATH", "/usr/local/lib/opencv2.4.7");
			pb.command(command);
System.out.println("====================== npm_match on " + imgpath);

			try {
				Process proc = pb.start();
				BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				String line;
				while ((line = stdInput.readLine()) != null) {
					System.out.println(imgpath + ">>>> " + line);
				}
				proc.waitFor();
System.out.println(imgpath + " DONE?????");
				////int returnCode = p.exitValue();

			} catch (Exception ioe) {
				System.out.println("oops: " + ioe.toString());
			}
			this.countComplete++;

			String c = "{ \"countComplete\": " + Integer.toString(this.countComplete) + ", \"countTotal\": " + Integer.toString(this.countTotal);
			if (this.countComplete >= this.countTotal) c += ", \"done\": true ";
			c += " }";
			writeStatusFile(this.servletContext, this.context, this.batchID, c);
		}

System.out.println("RETURN");
	}


  public void run() {
System.out.println("running, method=" + this.method);
		if (this.method.equals("npmProcess")) {
			npmProcess();
		} else {
			npmCompare();
		}

    status = Status.INIT;

  }


	public static boolean writeStatusFile(ServletContext servletContext, String context, String batchID, String contents) {
		String rootDir = servletContext.getRealPath("/");
		String baseDir = ServletUtilities.dataDir(context, rootDir);
		String batchDir = baseDir + "/match_images/" + batchID;

		try {
			PrintWriter statusOut = new PrintWriter(batchDir + "/status.json");
			statusOut.println(contents);
			statusOut.close();
		} catch (Exception ex) {
			System.out.println("could not write " + baseDir + "/status.json: " + ex.toString());
			return false;
		}

		return true;
	}


}