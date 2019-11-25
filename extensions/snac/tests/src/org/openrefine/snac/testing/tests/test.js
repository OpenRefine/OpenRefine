// in this directory, run 
// npm i puppeteer
// npm i puppeteer-core

//then, run node test.js to run the test

// note: must be running ./refine first

// extensions/snac/tests/src/org/openrefine/snac/testing/tests

// node extensions/snac/tests/src/org/openrefine/snac/testing/tests/test.js

// other useful examples: https://developers.google.com/web/tools/puppeteer/examples

const puppeteer = require("puppeteer");
// const chalk = require ("chalk");

//colors for console logs
// const error = chalk.bold.red;
// const success = chalk.keyword("green");

// const username = 'test';
// const password = 'test';

// const readline = require('readline');
// const rl = readline.createInterface({
//   input: process.stdin,
//   output: process.stdout
// });



  (async () => {
      try {
          var browser = await puppeteer.launch({
            headless: false,
            defaultViewport: null,
            devtools: true
          });
          var page = await browser.newPage();
  

          await page.goto(`http://127.0.0.1:3333/`);

          console.log("hi");
          var content = await page.content();

        //   var projectURL = "";

          var projectURL = await page.evaluate(() => {
            let elements = $('.action-area-tab').toArray();
            $(elements[1]).click(); 
            console.log("asdfa");

            let openProj = document.querySelectorAll('.project-name');
            // console.log(openProj);
            // console.log(openProj[0]);
            // console.log(openProj[0].href);

             return openProj[0].href;

            // console.log("open");
          });

        await console.log(projectURL);

        await page.goto(`${projectURL}`);

        await page.evaluate( () => {
          // let elements = document.getElementById( 'extension-bar-menu-container' );
          let button = document.getElementById('extension-bar-menu-container');
          // $(button[0]).click(); 
          console.log(button.childNodes.length);
        });
        // await page.evaluate(() => {
        //   console.log("peepeepoopoo")
        //   let elements = $('#extension-bar-menu-container').toArray();
        //   let button = elements[0].children.get;
        //   // $(button[0]).click(); 
        //   // console.log(button);
        //   console.log(button);
        //   // console.log(elements[0].children);
        //   // console.log(elements[0].childNodes[0]);


        // });

        // const selectElem = await page.$('div[name="menu-container"]');
        // await selectElem.type('Upload edits to SNAC');

          await page.waitFor(100000);
          await browser.close();
          console.log("Browser Closed");
      } catch (err) {
          console.log(err);
          await browser.close();
          console.log("Error: Browser Closed");
      }
  })();
 