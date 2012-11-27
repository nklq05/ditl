/*******************************************************************************
 * This file is part of DITL.                                                  *
 *                                                                             *
 * Copyright (C) 2011-2012 John Whitbeck <john@whitbeck.fr>                    *
 *                                                                             *
 * DITL is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU General Public License as published by        *
 * the Free Software Foundation, either version 3 of the License, or           *
 * (at your option) any later version.                                         *
 *                                                                             *
 * DITL is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of              *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the               *
 * GNU General Public License for more details.                                *
 *                                                                             *
 * You should have received a copy of the GNU General Public License           *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.       *
 *******************************************************************************/
package ditl.graphs;

import java.util.Set;

import ditl.*;

public final class EdgeEvent {
	
	public enum Type { UP, DOWN };
	
	final Integer id1;
	final Integer id2;
	final Type _type;
	
	public EdgeEvent(Integer i1, Integer i2, Type type){
		if ( i1 < i2 ){
			id1 = i1;
			id2 = i2;
		} else {
			id1 = i2;
			id2 = i1;
		}
		_type = type;
	}
	
	public EdgeEvent(Edge edge, Type type){
		id1 = edge.id1;
		id2 = edge.id2;
		_type = type;
	}
	
	public Integer id1(){
		return id1;
	}
	
	public Integer id2(){
		return id2;
	}
	
	public boolean isUp(){
		return _type == Type.UP;
	}
	
	public Edge edge(){
		return new Edge(id1,id2);
	}
	
	public static final class Factory implements ItemFactory<EdgeEvent>{
		@Override
		public EdgeEvent fromString(String s) {
			String[] elems = s.trim().split(" ");
			try {
				Integer id1 = Integer.parseInt(elems[0]);
				Integer id2 = Integer.parseInt(elems[1]);
				Type type = Type.valueOf(elems[2]);
				return new EdgeEvent(id1,id2,type);
				
			} catch ( Exception e ){
				System.err.println( "Error parsing '"+s+"': "+e.getMessage() );
				return null;
			}
		}
	}
	
	@Override
	public String toString(){
		return id1+" "+id2+" "+_type;
	}
	
	public static final class InternalGroupFilter implements Filter<EdgeEvent> {
		private Set<Integer> _group;
		public InternalGroupFilter(Set<Integer> group){ _group = group;}
		@Override
		public EdgeEvent filter(EdgeEvent item) {
			if ( _group.contains(item.id1) && _group.contains(item.id2) )
				return item;
			return null;
		}
	}

}
