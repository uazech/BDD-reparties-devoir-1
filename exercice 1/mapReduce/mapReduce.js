/**
 * @file Filtre les sorts en base de données en utilisant l'algorithme mapReduce de MongoDB
 * @author Baptiste Chezaubernard
 */
var url = "mongodb://localhost:27017/";
var dbKey = "test";

var MongoClient = require("mongodb").MongoClient;
MongoClient.connect(
  url,
  { useNewUrlParser: true },
  function(err, db) {
    if (err) throw err;
    var sortsDB = db.db(dbKey);

    var sorts = sortsDB.collection("sortBO");

    /**
     * Fonction map
     *   Récupère les sorts de wizard niveau 4 et verbaux uniquement
     * @this : les données d'un sort de wizard
     * @emits : les sorts de niveau 4 au format {id : 0, sort:{}}
     */
    var map = function() {
      var isWizardLevel4 = false;
      var isVerbalSpell = false;
      if (this.jobs) {
        for (i = 0; i < this.jobs.length; i++) {
          if (
            this.jobs[i].jobName.includes("wizard") &&
            this.jobs[i].jobLevel <= 4
          )
            isWizardLevel4 = true;
        }
      }

      if (this.components.length == 1 && this.components[0] == "V") {
        isVerbalSpell = true;
      }

      if (isWizardLevel4 && isVerbalSpell) {
        emit({ id: this._id, name: this.name }, 1);
      }
    };

    /**
     * Fonction Reduce
     *  Retourne les sorts pré-filtrés
     * @param key : la clé contenant les valeurs
     * @param values
     * @return key : la clé contenant les valeurs filtrés
     */
    var reduce = function(key, values) {
      return key;
    };

    // Appel de l'algorithme mapReduce de MongoDB
    sorts.mapReduce(map, reduce, { out: { inline: 1 } }, function(err, result) {
      if (err) throw err;
      console.log("resultats trouvés : ");
      console.log(result);
      console.log("fin du programme");
      db.close();
    });
  }
);
