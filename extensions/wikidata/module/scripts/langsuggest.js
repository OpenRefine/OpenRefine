/********************
 * LANGUAGE SUGGEST *
 ********************/

// This list was manually copied from Wikidata on 2019-05-31. It can be updated with the following command:
//
// curl "https://www.wikidata.org/w/api.php?action=query&meta=wbcontentlanguages&wbclcontext=monolingualtext&format=json&formatversion=2" | jq ".query.wbcontentlanguages[].code" | tr '\n' ','
//
// See https://stackoverflow.com/questions/46507037/how-to-get-all-allowed-languages-for-wikidata/46562061#46562061
// I don't think it is worth making every OpenRefine client perform this query at every startup, because it typically does
// not change very often.
//
// This is the list of language codes accepted for monolingualtext values. The language codes for terms are more restrictive.
// TODO: in the future, store different lists for term languages and monolingual text languages.
DEFAULT_LANGUAGES = [
  "aa", "ab", "abs", "ace", "ady", "ady-cyrl", "aeb", "aeb-arab", "aeb-latn", "af", "ak", "aln", "als", "am", "an", "ang", "anp",
  "ar", "arc", "arn", "arq", "ary", "arz", "as", "ase", "ast", "atj", "av", "avk", "awa", "ay", "az", "azb", "ba", "ban", "bar",
  "bbc", "bbc-latn", "bcc", "bcl", "be", "be-tarask", "bg", "bgn", "bh", "bho", "bi", "bjn", "bm", "bn", "bo", "bpy", "bqi", "br",
  "brh", "bs", "btm", "bto", "bug", "bxr", "ca", "cbk-zam", "cdo", "ce", "ceb", "ch", "cho", "chr", "chy", "ckb", "co", "cps", "cr",
  "crh", "crh-cyrl", "crh-latn", "cs", "csb", "cu", "cv", "cy", "da", "de", "de-at", "de-ch", "din", "diq", "dsb", "dtp", "dty",
  "dv", "dz", "ee", "egl", "el", "eml", "en", "en-ca", "en-gb", "eo", "es", "es-419", "et", "eu", "ext", "fa", "ff", "fi", "fit",
  "fj", "fo", "fr", "frc", "frp", "frr", "fur", "fy", "ga", "gag", "gan", "gan-hans", "gan-hant", "gcr", "gd", "gl", "glk", "gn",
  "gom", "gom-deva", "gom-latn", "gor", "got", "grc", "gsw", "gu", "gv", "ha", "hak", "haw", "he", "hi", "hif", "hif-latn", "hil",
  "ho", "hr", "hrx", "hsb", "ht", "hu", "hy", "hyw", "hz", "ia", "id", "ie", "ig", "ii", "ik", "ike-cans", "ike-latn", "ilo", "inh",
  "io", "is", "it", "iu", "ja", "jam", "jbo", "jut", "jv", "ka", "kaa", "kab", "kbd", "kbd-cyrl", "kbp", "kea", "kg", "khw", "ki",
  "kiu", "kj", "kjp", "kk", "kk-arab", "kk-cn", "kk-cyrl", "kk-kz", "kk-latn", "kk-tr", "kl", "km", "kn", "ko", "ko-kp", "koi",
  "kr", "krc", "kri", "krj", "krl", "ks", "ks-arab", "ks-deva", "ksh", "ku", "ku-arab", "ku-latn", "kum", "kv", "kw", "ky", "la",
  "lad", "lb", "lbe", "lez", "lfn", "lg", "li", "lij", "liv", "lki", "lmo", "ln", "lo", "loz", "lrc", "lt", "ltg", "lus", "luz", "lv",
  "lzh", "lzz", "mai", "map-bms", "mdf", "mg", "mh", "mhr", "mi", "min", "mk", "ml", "mn", "mni", "mnw", "mo", "mr", "mrj", "ms", "mt",
  "mus", "mwl", "my", "myv", "mzn", "na", "nah", "nan", "nap", "nb", "nds", "nds-nl", "ne", "new", "ng", "niu", "nl", "nn", "no", "nod",
  "nov", "nqo", "nrm", "nso", "nv", "ny", "nys", "oc", "olo", "om", "or", "os", "ota", "pa", "pag", "pam", "pap", "pcd", "pdc", "pdt",
  "pfl", "pi", "pih", "pl", "pms", "pnb", "pnt", "prg", "ps", "pt", "pt-br", "qu", "qug", "rgn", "rif", "rm", "rmy", "rn", "ro",
  "roa-tara", "ru", "rue", "rup", "ruq", "ruq-cyrl", "ruq-latn", "rw", "rwr", "sa", "sah", "sat", "sc", "scn", "sco", "sd", "sdc",
  "sdh", "se", "sei", "ses", "sg", "sgs", "sh", "shi", "shi-latn", "shi-tfng", "shn", "shy-latn", "si", "sje", "sk", "skr", "skr-arab",
  "sl", "sli", "sm", "sma", "smj", "smn", "sms", "sn", "so", "sq", "sr", "sr-ec", "sr-el", "srn", "srq", "ss", "st", "stq", "sty", "su",
  "sv", "sw", "szl", "ta", "tay", "tcy", "te", "tet", "tg", "tg-cyrl", "tg-latn", "th", "ti", "tk", "tl", "tly", "tn", "to", "tpi", "tr",
  "tru", "ts", "tt", "tt-cyrl", "tt-latn", "tum", "tw", "ty", "tyv", "tzm", "udm", "ug", "ug-arab", "ug-latn", "uk", "ur", "uz", "uz-cyrl",
  "uz-latn", "ve", "vec", "vep", "vi", "vls", "vmf", "vo", "vot", "vro", "wa", "war", "wo", "wuu", "xal", "xh", "xmf", "xsy", "yi", "yo",
  "yue", "za", "zea", "zgh", "zh", "zh-cn", "zh-hans", "zh-hant", "zh-hk", "zh-mo", "zh-my", "zh-sg", "zh-tw", "zu", "und", "mis",
  "mul", "zxx", "abe", "abq", "ami", "bnn", "brx", "chn", "cnr", "cop", "el-cy", "ett", "eya", "fkv", "fos", "fr-ca", "frm", "fro",
  "fuf", "gez", "gmy", "hai", "haz", "hbo", "kjh", "koy", "lag", "lkt", "lld", "mid", "mnc", "moe", "non", "nr", "nxm", "ood", "otk",
  "pjt", "ppu", "pwn", "pyu", "quc", "qya", "rar", "shy", "sia", "sjd", "sjk", "sjn", "sjt", "sju", "ssf", "syc", "tlb", "trv", "tzl",
  "uga", "umu", "uun", "xpu", "yap", "zun"];

$.suggest("langsuggest", {
  _init: function () {
    this._status.SELECT = "Select a language from the list:";
  },

  request: function (val, cursor) {
    var self = this;
    $.ajax({
      dataType: "jsonp",
      url: WikibaseManager.getSelectedWikibaseApi(),
      data: {
        action: "languagesearch",
        search: val,
        format: "json",
      },
      success: function (data) {
        self.getLanguageCodes(function (languageCodes) {
          var array = [];
          for (var key in data.languagesearch) {
            if (data.languagesearch.hasOwnProperty(key) && languageCodes.indexOf(key) !== -1) {
              array.push({id: key, name: key, search_name: data.languagesearch[key]});
            }
          }
          self.response(array);
        });
      },
    });
  },

  apiEndpointToLangCodes: {},

  getLanguageCodes: function (callback) {
    var self = this;
    var selectedApi = WikibaseManager.getSelectedWikibaseApi();
    if (self.apiEndpointToLangCodes[selectedApi]) {
      callback(self.apiEndpointToLangCodes[selectedApi]);
    } else {
      // try fetching the language codes
      // action=query&meta=wbcontentlanguages&wbclcontext=monolingualtext&format=json
      $.ajax({
        type: "POST",
        dataType: "jsonp",
        url: WikibaseManager.getSelectedWikibaseApi() +
            "?action=query&meta=wbcontentlanguages&wbclcontext=monolingualtext&format=json",
        success: function (data) {
          var result = [];
          var langs = data.query.wbcontentlanguages;
          for (var lang in langs) {
            if (langs.hasOwnProperty(lang)) {
              result.push(langs[lang].code);
            }
          }
          self.apiEndpointToLangCodes[selectedApi] = result;
          callback(result);
        },
        error: function () {
          self.apiEndpointToLangCodes[selectedApi] = DEFAULT_LANGUAGES;
          callback(DEFAULT_LANGUAGES);
        },
      });
    }
  },

  create_item: function(data, response_data) {
    var css = this.options.css;
    var li = $("<li>").addClass(css.item);
    var type = $("<div>").addClass("fbs-item-type").text(data.id);
    var native_name = this.get_native_name(data.id);
    var full_name = native_name ? native_name : data.search_name;
    var label = $("<label>").text(full_name);
    li.append($("<div>").addClass(css.item_name).append(type).append(label));
    return li;
  },
 
  get_native_name: function(lang_code) {
    var language = $.uls.data.languages[lang_code];
    if (language) {
        return language[2];
    }
  },
});

