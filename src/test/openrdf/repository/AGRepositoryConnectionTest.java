package test.openrdf.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnectionTest;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

import test.AGAbstractTest;
import test.Util;

public class AGRepositoryConnectionTest extends RepositoryConnectionTest {
	
	/**
	 * Location of local test data that isn't provided via TEST_DIR_PREFIX
	 */
	public static final String TEST_DATA_DIR = "src/test/";
	
	public AGRepositoryConnectionTest(String name) {
		super(name);
	}

	
	@Override
	protected Repository createRepository() throws Exception {
		return AGAbstractTest.sharedRepository();
	}

	@Override
	public void testDeleteDefaultGraph() throws Exception {
		super.testDeleteDefaultGraph();
	}
	
	@Override
	public void testDefaultContext() throws Exception {
		super.testDefaultContext();
	}
	
	@Override
	public void testDefaultInsertContext() throws Exception {
		super.testDefaultInsertContext();
	}
	
	public void testDefaultInsertContextNull()
			throws Exception
		{
			ContextAwareConnection con = new ContextAwareConnection(testCon);
			URI defaultGraph = null;
			con.setInsertContext(defaultGraph);
			con.add(vf.createURI("urn:test:s1"), vf.createURI("urn:test:p1"), vf.createURI("urn:test:o1"));
			con.prepareUpdate("INSERT DATA { <urn:test:s2> <urn:test:p2> \"l2\" }").execute();
			assertEquals(2, con.getStatements(null, null, null).asList().size());
			assertEquals(2, con.getStatements(null, null, null, defaultGraph).asList().size());
			assertEquals(2, size(defaultGraph));
			con.add(vf.createURI("urn:test:s3"), vf.createURI("urn:test:p3"), vf.createURI("urn:test:o3"), (Resource)null);
			con.add(vf.createURI("urn:test:s4"), vf.createURI("urn:test:p4"), vf.createURI("urn:test:o4"), vf.createURI("urn:test:other"));
			assertEquals(4, con.getStatements(null, null, null).asList().size());
			assertEquals(3, con.getStatements(null, null, null, defaultGraph).asList().size());
			assertEquals(4, testCon.getStatements(null, null, null, true).asList().size());
			assertEquals(3, size(defaultGraph));
			assertEquals(1, size(vf.createURI("urn:test:other")));
			con.prepareUpdate("DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }").execute();
			assertEquals(0, con.getStatements(null, null, null).asList().size());
			assertEquals(0, testCon.getStatements(null, null, null, true).asList().size());
			assertEquals(0, size(defaultGraph));
			assertEquals(0, size(vf.createURI("urn:test:other")));
		}

	private int size(URI defaultGraph)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException
		{
			TupleQuery qry = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * { ?s ?p ?o }");
			DatasetImpl dataset = new DatasetImpl();
			dataset.addDefaultGraph(defaultGraph);
			qry.setDataset(dataset);
			TupleQueryResult result = qry.evaluate();
			try {
				int count = 0;
				while(result.hasNext()) {
					result.next();
					count++;
				}
				return count;
			} finally {
				result.close();
			}
		}

	@Override
	public void testExclusiveNullContext() throws Exception {
		super.testExclusiveNullContext();
	}

    /**
     * TODO: query.evaluate() needs to be inside the try below, in the 
     * parent test it's not.
     * 
     * TODO: 512 statements are added all at once here, in the parent 
     * test there are 512 single adds (far slower, consider rfe10261 to
     * improve the performance of the unmodified parent test).   
     * 
     */
    @Override
	public void testOrderByQueriesAreInterruptable() throws Exception {
    	//super.testOrderByQueriesAreInterruptable();
		testCon.setAutoCommit(false);
		Collection<Statement> stmts = new ArrayList<Statement>();
		for (int index = 0; index < 512; index++) {
			stmts.add(new StatementImpl(RDFS.CLASS, RDFS.COMMENT, testCon.getValueFactory().createBNode()));
		}
		testCon.add(stmts);
		testCon.setAutoCommit(true);

		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL,
				"SELECT * WHERE { ?s ?p ?o . ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 . ?s3 ?p3 ?o3 } ORDER BY ?s1 ?p1 ?o1 LIMIT 1000");
		query.setMaxQueryTime(2);

		long startTime = System.currentTimeMillis();
		try {
			TupleQueryResult result = query.evaluate();
			result.hasNext();
			fail("Query should have been interrupted");
		}
		catch (QueryInterruptedException e) {
			// Expected
			long duration = System.currentTimeMillis() - startTime;

			assertTrue("Query not interrupted quickly enough, should have been ~2s, but was "
					+ (duration / 1000) + "s", duration < 5000);
		}
	}

	@Override
	public void testGetNamespaces() throws Exception {
		super.testGetNamespaces();
	}
	
	@Override
	public void testXmlCalendarZ() throws Exception {
		super.testXmlCalendarZ();
	}
	
	@Override
	public void testSES713() throws Exception {
		super.testSES713();
	}
	
	public void testBaseURIInQueryString() throws Exception {
		testCon.add(vf.createURI("urn:test:s1"), vf.createURI("urn:test:p1"), vf.createURI("urn:test:o1"));
		TupleQueryResult rs = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "BASE <urn:test:s1> SELECT * { <> ?p ?o }").evaluate();
		try {
			assertTrue(rs.hasNext());
		}finally {
			rs.close();
		}
	}
	
	public void testBaseURIInParam() throws Exception {
		testCon.add(vf.createURI("http://example.org/s1"), vf.createURI("urn:test:p1"), vf.createURI("urn:test:o1"));
		TupleQueryResult rs = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * { <s1> ?p ?o }", "http://example.org").evaluate();
		try {
			assertTrue(rs.hasNext());
		}finally {
			rs.close();
		}
	}
	
	public void testBaseURIInParamWithTrailingSlash() throws Exception {
		testCon.add(vf.createURI("http://example.org/s1"), vf.createURI("urn:test:p1"), vf.createURI("urn:test:o1"));
		TupleQueryResult rs = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * { <s1> ?p ?o }", "http://example.org/").evaluate();
		try {
			assertTrue(rs.hasNext());
		}finally {
			rs.close();
		}
	}
		
    @Test
	public void testHasStatementWithoutBNodes() throws Exception {
		testCon.add(name, name, nameBob);

		assertTrue("Repository should contain newly added statement", testCon
				.hasStatement(name, name, nameBob, false));
	}

    @Test
	public void testHasStatementWithBNodes() throws Exception {
		testCon.add(bob, name, nameBob);

		assertTrue("Repository should contain newly added statement", testCon
				.hasStatement(bob, name, nameBob, false));

	}

    @Test
	public void testAddGzipInputStreamNTriples() throws Exception {
		// add file default-graph.nt.gz to repository, no context
	    File gz = AGAbstractTest.createTempFile("default-graph.nt-", ".gz");
	    File nt = new File(TEST_DATA_DIR + "default-graph.nt");
	    Util.gzip(nt, gz);
		InputStream defaultGraph = new FileInputStream(gz);
		//RepositoryConnectionTest.class.getResourceAsStream(TEST_DIR_PREFIX + "default-graph.nt.gz");
		try {
			testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
		} finally {
			defaultGraph.close();
		}

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

	}

    @Test
	public void testAddZipFileNTriples() throws Exception {
		InputStream in = new FileInputStream(TEST_DATA_DIR + "graphs.nt.zip");

		testCon.add(in, "", RDFFormat.NTRIPLES);

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		assertTrue("alice should be known in the store", testCon.hasStatement(
				null, name, nameAlice, false));

		assertTrue("bob should be known in the store", testCon.hasStatement(
				null, name, nameBob, false));
	}

    @Test
	public void testAddReaderNTriples() throws Exception {
		InputStream defaultGraphStream = new FileInputStream(TEST_DATA_DIR
				+ "default-graph.nt");
		Reader defaultGraph = new InputStreamReader(defaultGraphStream, "UTF-8");

		testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);

		defaultGraph.close();

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		// add file graph1.nt to context1
		InputStream graph1Stream = new FileInputStream(TEST_DATA_DIR
				+ "graph1.nt");
		Reader graph1 = new InputStreamReader(graph1Stream, "UTF-8");

		try {
			testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
		} finally {
			graph1.close();
		}

		// add file graph2.nt to context2
		InputStream graph2Stream = new FileInputStream(TEST_DATA_DIR
				+ "graph2.nt");
		Reader graph2 = new InputStreamReader(graph2Stream, "UTF-8");

		try {
			testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
		} finally {
			graph2.close();
		}

		assertTrue("alice should be known in the store", testCon.hasStatement(
				null, name, nameAlice, false));

		assertFalse("alice should not be known in context1", testCon
				.hasStatement(null, name, nameAlice, false, context1));
		assertTrue("alice should be known in context2", testCon.hasStatement(
				null, name, nameAlice, false, context2));

		assertTrue("bob should be known in the store", testCon.hasStatement(
				null, name, nameBob, false));

		assertFalse("bob should not be known in context2", testCon
				.hasStatement(null, name, nameBob, false, context2));
		assertTrue("bib should be known in context1", testCon.hasStatement(
				null, name, nameBob, false, context1));

	}

    @Test
	public void testAddInputStreamNTriples() throws Exception {
		// add file default-graph.nt to repository, no context
		InputStream defaultGraph = new FileInputStream(TEST_DATA_DIR
				+ "default-graph.nt");

		try {
			testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
		} finally {
			defaultGraph.close();
		}

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		// add file graph1.nt to context1
		InputStream graph1 = new FileInputStream(TEST_DATA_DIR + "graph1.nt");

		try {
			testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
		} finally {
			graph1.close();
		}

		// add file graph2.nt to context2
		InputStream graph2 = new FileInputStream(TEST_DATA_DIR + "graph2.nt");

		try {
			testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
		} finally {
			graph2.close();
		}

		assertTrue("alice should be known in the store", testCon.hasStatement(
				null, name, nameAlice, false));

		assertFalse("alice should not be known in context1", testCon
				.hasStatement(null, name, nameAlice, false, context1));
		assertTrue("alice should be known in context2", testCon.hasStatement(
				null, name, nameAlice, false, context2));

		assertTrue("bob should be known in the store", testCon.hasStatement(
				null, name, nameBob, false));

		assertFalse("bob should not be known in context2", testCon
				.hasStatement(null, name, nameBob, false, context2));
		assertTrue("bib should be known in context1", testCon.hasStatement(
				null, name, nameBob, false, context1));

	}

    @Test
	public void testRecoverFromParseErrorNTriples() throws RepositoryException,
			IOException {
		String invalidData = "bad";
		String validData = "<http://example.org/foo#a> <http://example.org/foo#b> <http://example.org/foo#c> .";

		try {
			testCon.add(new StringReader(invalidData), "", RDFFormat.NTRIPLES);
			fail("Invalid data should result in an exception");
		} catch (RDFParseException e) {
			// Expected behaviour
		}

		try {
			testCon.add(new StringReader(validData), "", RDFFormat.NTRIPLES);
		} catch (RDFParseException e) {
			fail("Valid data should not result in an exception");
		}

		assertEquals("Repository contains incorrect number of statements", 1,
				testCon.size());
	}

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
	public void testSimpleTupleQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?name ?mbox");
		queryBuilder.append(" WHERE { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");

		TupleQueryResult result = testCon.prepareTupleQuery(
				QueryLanguage.SPARQL, queryBuilder.toString()).evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("name"));
				assertTrue(solution.hasBinding("mbox"));

				Value nameResult = solution.getValue("name");
				Value mboxResult = solution.getValue("mbox");

				assertTrue((nameAlice.equals(nameResult) || nameBob
						.equals(nameResult)));
				assertTrue((mboxAlice.equals(mboxResult) || mboxBob
						.equals(mboxResult)));
			}
		} finally {
			result.close();
		}
	}

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
	public void testSimpleTupleQueryUnicode() throws Exception {
		testCon.add(alexander, name, Александър);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?person");
		queryBuilder.append(" WHERE { ?person foaf:name '");
		queryBuilder.append(Александър.getLabel()).append("' .}");

		TupleQueryResult result = testCon.prepareTupleQuery(
				QueryLanguage.SPARQL, queryBuilder.toString()).evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("person"));
				assertEquals(alexander, solution.getValue("person"));
			}
		} finally {
			result.close();
		}
	}

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
	public void testPreparedTupleQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?name ?mbox");
		queryBuilder.append(" WHERE { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");

		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL,
				queryBuilder.toString());
		query.setBinding("name", nameBob);

		TupleQueryResult result = query.evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("name"));
				assertTrue(solution.hasBinding("mbox"));

				Value nameResult = solution.getValue("name");
				Value mboxResult = solution.getValue("mbox");

				assertEquals("unexpected value for name: " + nameResult,
						nameBob, nameResult);
				assertEquals("unexpected value for mbox: " + mboxResult,
						mboxBob, mboxResult);
			}
		} finally {
			result.close();
		}
	}

	public void testPreparedTupleQuery2()
			throws Exception
		{
			testCon.add(alice, name, nameAlice, context2);
			testCon.add(alice, mbox, mboxAlice, context2);
			testCon.add(context2, publisher, nameAlice);

			testCon.add(bob, name, nameBob, context1);
			testCon.add(bob, mbox, mboxBob, context1);
			testCon.add(context1, publisher, nameBob);

			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
			queryBuilder.append(" SELECT ?name ?mbox");
			queryBuilder.append(" WHERE { ?x foaf:name ?name;");
			queryBuilder.append("            foaf:mbox ?mbox .}");

			TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder.toString());
			query.setBinding("x", bob);

			TupleQueryResult result = query.evaluate();

			try {
				assertTrue(result != null);
				assertTrue(result.hasNext());

				while (result.hasNext()) {
					BindingSet solution = result.next();
					assertTrue(solution.hasBinding("name"));
					assertTrue(solution.hasBinding("mbox"));

					Value nameResult = solution.getValue("name");
					Value mboxResult = solution.getValue("mbox");

					assertEquals("unexpected value for name: " + nameResult, nameBob, nameResult);
					assertEquals("unexpected value for mbox: " + mboxResult, mboxBob, mboxResult);
				}
			}
			finally {
				result.close();
			}
		}

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
	public void testPreparedTupleQueryUnicode() throws Exception {
		testCon.add(alexander, name, Александър);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?person");
		queryBuilder.append(" WHERE { ?person foaf:name '");
		queryBuilder.append(Александър.getLabel()).append("' .}");

		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL,
				queryBuilder.toString());
		query.setBinding("name", Александър);

		TupleQueryResult result = query.evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("person"));
				assertEquals(alexander, solution.getValue("person"));
			}
		} finally {
			result.close();
		}
	}

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
	public void testSimpleGraphQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" CONSTRUCT { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");
		queryBuilder.append(" WHERE { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");

		GraphQueryResult result = testCon.prepareGraphQuery(
				QueryLanguage.SPARQL, queryBuilder.toString()).evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				Statement st = result.next();
				if (name.equals(st.getPredicate())) {
					assertTrue(nameAlice.equals(st.getObject())
							|| nameBob.equals(st.getObject()));
				} else {
					assertTrue(mbox.equals(st.getPredicate()));
					assertTrue(mboxAlice.equals(st.getObject())
							|| mboxBob.equals(st.getObject()));
				}
			}
		} finally {
			result.close();
		}
	}

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
	public void testPreparedGraphQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" CONSTRUCT { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");
		queryBuilder.append(" WHERE { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");

		GraphQuery query = testCon.prepareGraphQuery(QueryLanguage.SPARQL,
				queryBuilder.toString());
		query.setBinding("name", nameBob);

		GraphQueryResult result = query.evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				Statement st = result.next();
				assertTrue(name.equals(st.getPredicate())
						|| mbox.equals(st.getPredicate()));
				if (name.equals(st.getPredicate())) {
					assertTrue("unexpected value for name: " + st.getObject(),
							nameBob.equals(st.getObject()));
				} else {
					assertTrue(mbox.equals(st.getPredicate()));
					assertTrue("unexpected value for mbox: " + st.getObject(),
							mboxBob.equals(st.getObject()));
				}

			}
		} finally {
			result.close();
		}
	}

    /**
     * AllegroGraph doesn't support SeRQL; test passes if
     * server reports that SeRQL is unsupported.
     */
    @Test
    @Override
	public void testPrepareSeRQLQuery() throws Exception {
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" SELECT person");
		queryBuilder.append(" FROM {person} foaf:name {").append(Александър.getLabel()).append("}");
		queryBuilder.append(" USING NAMESPACE foaf = <" + FOAF_NS + ">");

		try {
			testCon.prepareQuery(QueryLanguage.SERQL, queryBuilder.toString());
		}
		catch (UnsupportedOperationException e) {
			// expected
		}
		catch (ClassCastException e) {
			fail("unexpected query object type: " + e.getMessage());
		}

    }

}
