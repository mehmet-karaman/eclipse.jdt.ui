/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;


class JavaCompareUtilities {
	
	public static ImageDescriptor getImageDescriptor(String relativePath) {
		
		JavaPlugin plugin= JavaPlugin.getDefault();

		URL installURL= null;
		if (plugin != null)
			installURL= plugin.getDescriptor().getInstallURL();
					
		if (installURL != null) {
			try {
				URL url= new URL(installURL, "icons/full/" + relativePath); //$NON-NLS-1$
				return ImageDescriptor.createFromURL(url);
			} catch (MalformedURLException e) {
				Assert.isTrue(false);
			}
		}
		return null;
	}
	
	static JavaTextTools getJavaTextTools() {
		JavaPlugin plugin= JavaPlugin.getDefault();
		if (plugin != null)
			return plugin.getJavaTextTools();
		return null;
	}
	
	static IDocumentPartitioner createJavaPartitioner() {
		JavaTextTools tools= getJavaTextTools();
		if (tools != null)
			return tools.createDocumentPartitioner();
		return null;
	}

	/**
	 * Returns null if an error occurred.
	 */
	static String readString(InputStream is) {
		if (is == null)
			return null;
		BufferedReader reader= null;
		try {
			StringBuffer buffer= new StringBuffer();
			char[] part= new char[2048];
			int read= 0;
			reader= new BufferedReader(new InputStreamReader(is));

			while ((read= reader.read(part)) != -1)
				buffer.append(part, 0, read);
			
			return buffer.toString();
			
		} catch (IOException ex) {
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
					JavaPlugin.log(ex);
				}
			}
		}
		return null;
	}
	
	/**
	 * Breaks the given string into lines and strips off the line terminator.
	 */
	static String[] readLines(InputStream is) {
		
		String[] lines= null;
		try {
			StringBuffer sb= null;
			List list= new ArrayList();
			while (true) {
				int c= is.read();
				if (c == -1)
					break;
				if (c == '\n' || c == '\r') {
					if (sb != null)
						list.add(sb.toString());
					sb= null;
				} else {
					if (sb == null)
						sb= new StringBuffer();
					sb.append((char)c);
				}
			}
			if (sb != null)
				list.add(sb.toString());
			return (String[]) list.toArray(new String[list.size()]);

		} catch (IOException ex) {
			return null;

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ex) {
				}
			}
		}
	}
}
