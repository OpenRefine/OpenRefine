import fs from 'fs';
import path from 'path';

const FROM_DIR = path.join('node_modules');
const TO_DIR = path.join('modules', 'core', '3rdparty');

if (!fs.existsSync(TO_DIR)) {
    fs.mkdirSync(TO_DIR);
    console.log('Directory ' + TO_DIR + ' created.');
}

try {
    const data = fs.readFileSync('dependencies.json', 'utf8');
    const dependencies = JSON.parse(data);

    // mkdir for all 'directories' listed in 'dependencies.json' (relative to TO_DIR)
    const DIRS = dependencies.directories;
    for (const dir of DIRS) {
        const dirPath = path.join(TO_DIR, dir);
        if (!fs.existsSync(dirPath)) {
            fs.mkdirSync(dirPath);
            console.log('Directory ' + dirPath + ' created.');
        }
    }

    // cp 'files' listed in 'dependencies.json' (from / to paths are relative to FROM_DIR / TO_DIR)
    const PATHS = dependencies.files;
    for (const item of PATHS) {
        const from = item.from;
        const fromPath = path.join(FROM_DIR, from);
        const to = item.to === '' ? from : item.to;
        const toPath = path.join(TO_DIR, to);

        fs.copyFileSync(fromPath, toPath);
        console.log(fromPath + ' was copied to ' + toPath);
    }

} catch (err) {
    console.log(`Error reading dependencies.json: ${err}`);
    process.exit(-1);
}
