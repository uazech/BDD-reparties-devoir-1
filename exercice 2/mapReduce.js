/**
 * @file Calcul du pageRank d'un graphe de point en utilisant l'algorithme mapReduce de MongoDB
 * @author Baptiste Chezaubernard
 */
const MongoClient = require("mongodb").MongoClient;

const url = "mongodb://localhost:27017/";
const dbKey = "matrix";

const graph = [
  { _id: "A", value: { adjlist: ["B", "C"], rank: 1 } },
  { _id: "B", value: { adjlist: ["C"], rank: 1 } },
  { _id: "C", value: { adjlist: ["A"], rank: 1 } },
  { _id: "D", value: { adjlist: ["C"], rank: 1 } }
];

MongoClient.connect(
  url,
  { useNewUrlParser: true },
  function(err, db) {
    if (err) throw err;
    var matrixDb = db.db(dbKey);
    var collection = matrixDb.collection("graph");
    collection.deleteMany();

    collection.insertMany(graph, { w: 1 }).then(function(result) {
      /**
       * Fonction Map :
       *  Calcul la probabilité d'aller sur les points en sortie à partir du point en entrée
       * @this : les informations du point en entrée (au format { _id: "A", value: { adjlist: ["B", "C"], rank: 1 } })
       * @emits : (point, listPointsAdjacents)
       * @emits : (point, probabilité d'aller sur ce point)
       * @emits : (point, 0)
       */
      var map = function() {
        var adjlist = this.value.adjlist;
        var id = this._id;
        for (i = 0; i < adjlist.length; i++) {
          emit(adjlist[i], this.value.rank / adjlist.length);
        }
        emit(id, adjlist);
        emit(id, 0);
      };

      /**
       * Fonction Reduce :
       *  Calcul le pageRank d'un point
       * @param {*} key : le nom du point
       * @param {*} values : la probabilité d'aller sur ce point à partir des autres points
       * @returns un objet au format { adjlist: adjlist, rank: pageRank } pour insertion en base
       */
      var reduce = function(key, values) {
        const DAMPING_FACTOR = 0.85;

        var pageRank = 0.0;
        var adjlist = [];
        for (i = 0; i < values.length; i++) {
          if (values[i] instanceof Array) {
            adjlist = values[i]; // Si la valeur est du type array, alors elle représente la matrice d'adjacence qui va être utilisée pour la réinséertion dans MongoDB
          } else {
            pageRank += values[i]; // Sinon, c'est qu'elle représente le pagerank recalculé
          }
        }
        pageRank = 1 - DAMPING_FACTOR + DAMPING_FACTOR * pageRank;
        return { adjlist: adjlist, rank: pageRank };
      };
      /**
       * Fonction iterate
       *   Réalise les itérations pour le pageRank
       * @param {*} i : l'indice courant
       */
      function iterate(i) {
        collection.mapReduce(
          map,
          reduce,
          {
            out: {
              replace: "graph"
            }
          },
          function(err, result) {
            if (err) throw err;
            // Récupère les données en base pour afficher la progression des itérations
            collection
              .find()
              .toArray()
              .then(function(data) {
                console.log("iteration " + i, data);
                if (i < 20) {
                  iterate(i + 1);
                } else {
                  console.log("Fin du programme");
                  db.close();
                }
              });
          }
        );
      }
      iterate(0);
    });
  }
);
