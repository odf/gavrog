/*
   Copyright 2013 Olaf Delgado-Friedrichs

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package org.gavrog.apps._3dt;

public class Version {
	final public static int major = 0;

	final public static int minor = 6;

	final public static int patchLevel = 0;

	final public static String maturity = "beta";

	final public static String date = "2013/08/16";

	final public static String extension = (maturity == null ? "" : " "
            + maturity)
            + (date == null ? "" : " as of " + date);

    final public static String full = major + "." + minor + "." + patchLevel
            + extension;
}
