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

package org.ensembl.healthcheck.util;

import java.util.*;

/**
 * Implementation of Comparator for comparing Integer objects.
 */

public class IntegerComparator implements Comparator {
  
  public int compare(Object o1, Object o2) {
    
    int i1 = ((Integer)o1).intValue();
    int i2 = ((Integer)o2).intValue();
    
    int result = 0;
    if (i1 == i2) {
      result = 0;
    } else if (i1 < i2) {
      result = -1;
    } else if (i1 > i2) {
      result = 1;
    }
    
    return result;
    
  }
  
}
