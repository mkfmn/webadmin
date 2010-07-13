package org.neo4j.webadmin.gremlin.tmpimpl;


import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.StringFactory;
import org.neo4j.graphdb.Relationship;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class Neo4jEdge extends Neo4jElement implements Edge {

    public Neo4jEdge(final Relationship relationship, final Neo4jGraphTemp graph) {
        super(graph);
        this.element = relationship;
    }

    public String getLabel() {
        return ((Relationship) this.element).getType().name();
    }

    public Vertex getOutVertex() {
        return new Neo4jVertex(((Relationship) this.element).getStartNode(), this.graph);
    }

    public Vertex getInVertex() {
        return new Neo4jVertex(((Relationship) this.element).getEndNode(), this.graph);
    }

    public boolean equals(final Object object) {
        return object instanceof Neo4jEdge && ((Neo4jEdge) object).getId().equals(this.getId());
    }

    public String toString() {
        return StringFactory.edgeString(this);
    }
}
