/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.core.resource;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Utility class that attempts to locate files.
 * 
 * @author Mayo Clinic
 */
final public class FileLocator {

   static private final Logger LOGGER = Logger.getLogger( "FileLocator" );

   /**
    * $CTAKES_HOME is an environment variable that may be set to indicate
    * the absolute directory path of the ctakes installation
    */
   static private final String CTAKES_HOME = "CTAKES_HOME";

   private FileLocator() {
   }

    public static InputStream getAsStream(String location) throws FileNotFoundException
    {
        try
        {
        	//Get from classpath
        	InputStream is  = FileLocator.class.getClassLoader().getResourceAsStream(location);
        	if (is==null) throw new RuntimeException("Unable to locate " + location + " on classpath.");
        	return is;
        }
        catch (Exception e)
        {
        	//Try to get from filestream, locating relative to the current directory if 
        	// location is a relative path
        	File f = new File(location);
        	FileInputStream fs = new FileInputStream(f);
        	return fs;
        }
    }
	
    /**
     * Where a Stream is usable, use {@link #getAsStream(String)} .
     * Where a path String is usable, use {@link #getFullPath(String)} .
     */
    public static File locateFile( final String location ) throws FileNotFoundException {
       final String fullPath = getFullPath( location );
       final File file = new File( fullPath );
       if ( !file.exists() ) {
          throw new FileNotFoundException( "No File at " + location );
       }
       return file;
    }

   /**
    * Logs a debug message before returning the absolute path of a file derived from some relative path
    *
    * @param relativePath relative path of some file
    * @param file         the actual file addressed by relative path
    * @param locationText description of where file exists relative to relativePath
    * @return the canonical path of file or the absolute path of file if the canonical cannot be made
    */
   static private String createDiscoveredPath( final String relativePath, final File file, final String locationText ) {
      try {
         LOGGER.debug( relativePath + " discovered " + locationText + " as: " + file.getCanonicalPath() );
         return file.getCanonicalPath();
      } catch ( IOException ioE ) {
         LOGGER.debug( relativePath + " discovered " + locationText + " as: " + file.getPath() );
         return file.getPath();
      }
   }

   /**
    * Attempts to discover the real location of a file pointed to by relativePath.
    * The search will be performed in the following order:
    * <p>
    * 1. By checking to see if the provided relative path is actually an absolute path
    * 2. By checking within the ClassPath
    * 3. By checking directly under the current working directory
    * 4. By checking under $CTAKES_HOME
    * 5. By traversing above the current working directory.  Useful when running under a module directory in an IDE
    * Example:  cwd = /usr/bin/ctakes/ctakes-module , relativePath = ctakes-other-module/more/file.ext
    * The directory above cwd /usr/bin/ctakes will be checked for containment of the relative path
    * If /usr/bin/ctakes/ctakes-other-module/more/file.txt exists then that is returned
    * 6. By traversing above the current working directory and under a subdirectory ctakes/
    * Example: cwd = /usr/bin/my_custom_ctakes/my_ctakes-module , relativePath = ctakes-other-module/more/file.ext
    * The directory above cwd /usr/bin will be checked for containment of ctakes/ plus the relative path
    * If /usr/bin/ctakes/ctakes-other-module/more/file.txt exists then that is returned
    * </p>
    *
    * @param relativePath some relative path to a file
    * @return the canonical path of the file or the absolute path of the file if the canonical cannot be made
    * @throws FileNotFoundException if the file cannot be found
    */
   static public String getFullPath( final String relativePath ) throws FileNotFoundException {
      File file = new File( relativePath );
      if ( file.exists() ) {
         return createDiscoveredPath( relativePath, file, "without adjustment" );
      }
      // check in the classpath
      try {
         file = locateOnClasspath( relativePath );
         if ( file.exists() ) {
            return createDiscoveredPath( relativePath, file, "under Classpath" );
         }
      } catch ( FileNotFoundException | URISyntaxException multiE ) {
         // the locateOnClasspath method throws exceptions if the file isn't found.  Ignore and continue
      }
      // check for relative directly under current working directory
      final String cwd = System.getProperty( "user.dir" );
      file = new File( cwd, relativePath );
      if ( file.exists() ) {
         return createDiscoveredPath( relativePath, file, "under Working Directory" );
      }
      // Check under the $CTAKES_HOME location  Do this before messing with relative path traversal
      final String cTakesHome = System.getenv( CTAKES_HOME );
      if ( cTakesHome != null && !cTakesHome.isEmpty() ) {
         file = new File( cTakesHome, relativePath );
         if ( file.exists() ) {
            return createDiscoveredPath( relativePath, file, "under $CTAKES_HOME" );
         }
      }
      // Users running projects out of an ide may have the module directory as cwd
      // OR in a personal project directory parallel to that of the ctakes installation
      File cwdDerived = new File( cwd );
      while ( cwdDerived.getParentFile() != null ) {
         cwdDerived = cwdDerived.getParentFile();
         file = new File( cwdDerived, relativePath );
         if ( file.exists() ) {
            return createDiscoveredPath( relativePath, file, "above Working Directory" );
         }
         file = new File( cwdDerived, "ctakes/" + relativePath );
         if ( file.exists() ) {
            return createDiscoveredPath( relativePath, file, "above Working Directory /ctakes" );
         }
      }
      final StringBuilder sb = new StringBuilder();
      sb.append( "Could not find " ).append( relativePath ).append( "\nas absolute or in $CLASSPATH :\n" );
      final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
      final URL[] classpathUrls = ((URLClassLoader)classLoader).getURLs();
      for ( URL url : classpathUrls ) {
         sb.append( url.getFile() ).append( "\n" );
      }
      sb.append( "or in working directory : " ).append( cwd ).append( "\n" );
      sb.append( "or in any parent thereof (with or without /ctakes/)\n" );
      sb.append( "or in $CTAKES_HOME : " ).append( cTakesHome );
      LOGGER.error( sb.toString() );
      throw new FileNotFoundException( "No File exists at " + relativePath );
   }

   /**
    * Check the java classpath for the presence of a file pointed to by relativePath
    *
    * @param relativePath some relative path to a file
    * @return a file in the classpath pointed to by relativePath - if found
    * @throws FileNotFoundException if the file is not found in the classpath
    * @throws URISyntaxException    if the discovered file cannot be converted into a URI
    */
   private static File locateOnClasspath( final String relativePath )
         throws FileNotFoundException, URISyntaxException {
      final ClassLoader classLoader = FileLocator.class.getClassLoader();
      final URL indexUrl = classLoader.getResource( relativePath );
      if ( indexUrl == null ) {
         throw new FileNotFoundException( relativePath );
      }
      final URI indexUri = new URI( indexUrl.toExternalForm() );
      if ( indexUri.isOpaque() ) {
         throw new FileNotFoundException( relativePath );
      }
      return new File( indexUri );
   }

}