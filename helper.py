import argparse
import hashlib
import os
import sys
from subprocess import call
import time
import json

BUF_SIZE = 65535

MD5 = 0

SOURCE_DIR = os.path.abspath("src/main/kotlin/io/github/gryjo/halite")
SUBMISSION_DIR = os.path.abspath("build/distributions")
HASH_SAVE_FILE = os.path.abspath("halite.md5")
REPLAY_DIR = os.path.abspath("replays")


def dir_hash(root_dir: str, hash_type: int = MD5, topdown: bool = True) -> str:
    hashed_files = {}

    for root, dirs, files in os.walk(root_dir, topdown=topdown):
        for name in files:
            abs_path = os.path.normpath(os.path.join(root, name))
            hashed_files[abs_path] = file_hash(abs_path, hash_type)

    hasher = None
    if hash_type == MD5:
        hasher = hashlib.md5()

    [hasher.update(("%s%s" % (k, v)).encode(sys.getdefaultencoding())) for k, v in hashed_files.items()]
    return hasher.hexdigest()


def file_hash(file: str, hash_type: int = MD5) -> str:
    hasher = None
    if hash_type == MD5:
        hasher = hashlib.md5()

    with open(file, "rb") as f:
        while True:
            data = f.read(BUF_SIZE)
            if not data:
                break
            hasher.update(data)
    return hasher.hexdigest()


def read_hash(file: str) -> str:
    if not os.path.exists(HASH_SAVE_FILE):
        return None
    with open(file, "r") as f:
        old_hash = f.readline()
    return old_hash


def save_hash(file: str, new_hash: str):
    with open(file, "w") as f:
        f.write(new_hash)


def build(force: bool = False) -> bool:
    new_hash = dir_hash(SOURCE_DIR)
    old_hash = read_hash(HASH_SAVE_FILE)
    if old_hash and old_hash == new_hash:
        print("No new build necessary: Content hasn't changed")
        if force:
            print("FORCING REBUILD")
        else:
            return True
    if not old_hash:
        print("No hash saved!")
    print("Starting build!")
    start = time.perf_counter()
    success = False
    with os.popen("gradle build") as s:
        if "BUILD SUCCESSFUL" in s.read():
            success = True
    save_hash(HASH_SAVE_FILE, new_hash)
    delta = time.perf_counter() - start
    print("BUILD {:s} ({:.3f}s)".format("SUCCESSFUL" if success else "FAILED", delta))
    return success


def run(force: bool = False):
    success = build(force)
    if not success:
        print("ABORTING: Build was not successful")
        return
    print()
    print("Starting Run!")
    start = time.perf_counter()
    cmd = 'halite -i "{:s}" -q -d "240 160" "java -jar build/libs/MyBot.jar" "java -jar build/libs/MyBot.jar"'\
        .format(REPLAY_DIR)
    with os.popen(cmd) as s:
        data = json.loads(s.read())
    delta = time.perf_counter() - start
    print("RUN DONE ({:.3f}s)!".format(delta))
    print('REPLAY SAVED: "{:s}"'.format(data["replay"]))


def submission(force: bool = False) -> bool:
    success = build(force)
    if not success:
        print("ABORTING: Build was not successful")
        return False
    print("Starting submission build!")
    start = time.perf_counter()
    success = False
    with os.popen("gradle submission") as s:
        if "BUILD SUCCESSFUL" in s.read():
            success = True
    delta = time.perf_counter() - start
    print("SUBMISSION {:s} ({:.3f}s)".format("SUCCESSFUL" if success else "FAILED", delta))
    os.popen('explorer "{:s}"'.format(SUBMISSION_DIR))
    return success


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Handles build/run related stuff for halite')
    parser.add_argument('type', type=str, choices=["build", "run", "submission"])
    parser.add_argument("-f", "--force", type=bool, help="forces rebuild", default=False, const=True, nargs="?")
    args = parser.parse_args()

    if args.type == "build":
        build(args.force)
    elif args.type == "run":
        run(args.force)
    elif args.type == "submission":
        submission(args.force)
