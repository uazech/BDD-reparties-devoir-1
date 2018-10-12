package com.crawler.step.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.crawler.modele.JobBO;
import com.crawler.modele.SortBO;
import com.crawler.repository.sqlite.SortMongoRepository;

@Component
public class SqliteTasklet implements Tasklet {

	private static final Logger LOG = LoggerFactory.getLogger(SqliteTasklet.class);

	@Autowired
	private SortMongoRepository sortMongoRepository;

	private String database = "sorts.db";

	private String URL = "jdbc:sqlite:" + database;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		LOG.info("Execution des commandes Sqlite");
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1) {

		}
		List<SortBO> listeSorts = sortMongoRepository.findAll();
		deleteDatabase();
		createNewDatabase();
		createTables();
		insertsTable(listeSorts);
		LOG.info("Résultats : ");
		try (Connection conn = this.connect()) {
			getListSorts(conn);
		} catch (SQLException ex) {
			LOG.error(ex.getMessage(), ex);
		}
		return RepeatStatus.FINISHED;
	}

	/**
	 * Insertion dans les tables
	 * 
	 * @param listeSorts
	 */
	private void insertsTable(List<SortBO> listeSorts) {
		LOG.info("Insertion dans les tables de données (~5mins)");
		try (Connection conn = this.connect()) {
			for (SortBO sort : listeSorts) {
				LOG.debug("Insertion du sort " + sort.getId() + " dans la base sort.db");
				insertSort(sort, conn);
				for (JobBO jobLevel : sort.getJobs()) {
					insertJobLevel(jobLevel, sort.getId(), conn);
				}
				insertComponents(sort, conn, sort.getId());

			}
		} catch (SQLException ex) {
			LOG.error(ex.getMessage(), ex);
		}
	}

	/**
	 * Insertions dans la table jobLevel
	 * 
	 * @param job
	 *            : le job/level à inserer
	 * @param idSort
	 *            : l'identifiant du sort
	 * @param conn
	 *            : la connection à la DB
	 */
	private void insertJobLevel(JobBO job, int idSort, Connection conn) {
		try (PreparedStatement pstmt = conn
				.prepareStatement("INSERT INTO JOB_LEVEL (name, level, id_sort) VALUES (?, ?, ?)")) {
			pstmt.setString(1, job.getJobName());
			pstmt.setInt(2, job.getJobLevel()); // TODO : récupérer le job/level id
			pstmt.setInt(3, idSort);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			LOG.info(e.getMessage());
		}
	}

	/**
	 * Insertions dans la table sort
	 * 
	 * @param sort
	 *            : le sort à insérer
	 * @param conn
	 *            : la connection
	 */
	private void insertSort(SortBO sort, Connection conn) {
		try (PreparedStatement pstmt = conn
				.prepareStatement("INSERT INTO SORT (id, spell_resistance, name) VALUES (?, ?, ?)")) {
			pstmt.setInt(1, sort.getId());
			pstmt.setBoolean(2, sort.isSpellResistance());
			pstmt.setString(3, sort.getName());
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Insertions dans la table component
	 * 
	 * @param sort
	 *            : le sort pour lequel on souhait insérer les composants
	 * @param conn
	 *            la connection
	 * @param idSort
	 *            l'identifiant du sort
	 * @throws SQLException
	 */
	private void insertComponents(SortBO sort, Connection conn, int idSort) throws SQLException {
		for (String component : sort.getComponents()) {
			int idComponent = getComponentId(component, conn);
			if (idComponent == -1) {
				insertComponent(component, conn);
				idComponent = getComponentId(component, conn);
			}
			insertComponentSort(idSort, idComponent, conn);
		}
	}

	/**
	 * Insertion dans la table component_sort
	 * 
	 * @param idSort
	 *            : l'identifiant du sort à insérer
	 * @param idComponent
	 *            l'identifiant du composant
	 * @param conn
	 *            la connection
	 */
	private void insertComponentSort(int idSort, int idComponent, Connection conn) {
		try (PreparedStatement pstmt = conn
				.prepareStatement("INSERT INTO COMPONENT_SORT (id_sort, id_component) VALUES (?, ?)")) {
			pstmt.setInt(1, idSort);
			pstmt.setInt(2, idComponent);

			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Insertion d'une ligne dans la table component
	 * 
	 * @param component
	 *            le nom du composant
	 * @param conn
	 *            la connection à la DB
	 */
	private void insertComponent(String component, Connection conn) {
		try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO COMPONENT (label) VALUES (?)")) {
			pstmt.setString(1, component);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Récupère l'identifiant d'un composant
	 * 
	 * @param component
	 *            : le nom du composant
	 * @param conn
	 *            la connection
	 * @return l'idenfiant du composant
	 * @throws SQLException
	 */
	private int getComponentId(String component, Connection conn) throws SQLException {
		try (PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM COMPONENT WHERE label=?")) {
			pstmt.setString(1, component);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				return rs.getInt("id");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return -1;
	}

	/**
	 * Affiche la liste des sorts verbaux uniquement, de niveau 4 pour les wizards
	 * 
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	private int getListSorts(Connection conn) throws SQLException {
		LOG.info("Selection des sorts");
		try (PreparedStatement pstmt = conn.prepareStatement(
				"SELECT DISTINCT(id_sort), S.name FROM SORT S INNER JOIN job_level JL on JL.id_sort=S.id WHERE S.id IN (SELECT id_sort FROM component_sort GROUP BY id_SORT HAVING id_component=(SELECT C.id FROM component C WHERE label='V')) AND JL.level<=4 AND JL. name LIKE '%wizard%';")) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				LOG.info("Sort trouvé : " + rs.getInt("id_sort") + " - " + rs.getString("name"));
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return -1;
	}

	/**
	 * Supprime la base de données
	 */
	private void deleteDatabase() {
		File file = new File(database);
		if (file.exists()) {
			if (file.delete()) {
				LOG.info("Suppression de la base de données SQLite");
			}

		}
	}

	/**
	 * Créé les tables
	 * 
	 * @throws SQLException
	 */
	private void createTables() throws SQLException {
		LOG.info("Création des tables de données");

		String createTableSorts = "CREATE TABLE sort ( id INTEGER PRIMARY KEY AUTOINCREMENT, spell_resistance TEXT, name TEXT );";
		String createTableComponent = "CREATE TABLE component ( id INTEGER PRIMARY KEY AUTOINCREMENT, label TEXT );";
		String createTableComponentSort = "CREATE TABLE component_sort ( id_sort INTEGER , id_component INTEGER, PRIMARY KEY(id_sort, id_component), FOREIGN KEY (id_sort) REFERENCES sort(id), FOREIGN KEY (id_component) REFERENCES component(id_component) );";
		String createTableJobLevel = "CREATE TABLE job_level ( id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, level INTEGER, id_sort INTEGER, FOREIGN KEY(id_sort) REFERENCES sort(id) );";
		try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
			stmt.execute(createTableSorts);
			stmt.execute(createTableComponent);
			stmt.execute(createTableComponentSort);
			stmt.execute(createTableJobLevel);
		}
	}

	/**
	 * Créé une nouvelle base de données
	 */
	private void createNewDatabase() {
		LOG.info("Création de la base de données SQLite");

		try (Connection conn = DriverManager.getConnection(URL)) {
			if (conn != null) {
				conn.getMetaData();
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Récupère la connection à la BDD
	 * 
	 * @return
	 */
	private Connection connect() {
		// SQLite connection string
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(URL);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return conn;
	}

}
