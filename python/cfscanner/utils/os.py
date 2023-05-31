import os
import platform
import sys


def detect_system() -> tuple:
    """detects the current system and returns a tuple of the form (system, arch, abi)

    Raises:
        OSError: if the system is not supported

    Returns:
        tuple: (system, arch, abi)
    """
    current_system = platform.system().lower()
    current_machine = platform.machine().lower()
    if current_system == "linux":
        if hasattr(sys, "getandroidapilevel"):
            return ('android', 'arm64', 'v8a')
        if "arm" in current_machine or "aarch" in current_machine:
            if "v5" in current_machine:
                return ('linux', 'arm32', 'v5')
            elif "v6" in current_machine:
                return ('linux', 'arm32', 'v6')
            elif "v7" in current_machine:
                return ('linux', 'arm32', 'v7a')
            elif "aarch64" in current_machine:
                return ('linux', 'arm64', 'v8a')
        elif "mips" in current_machine:
            if "le" in current_machine:
                return ('linux', 'mips32le')
            else:
                return ('linux', 'mips32')
        elif "ppc64le" in current_machine:
            return ('linux', 'ppc64le')
        elif "ppc64" in current_machine:
            return ('linux', 'ppc64')
        elif "s390x" in current_machine:
            return ('linux', 's390x')
        elif "riscv64" in current_machine:
            return ('linux', 'riscv64')
        else:
            if "64" in current_machine:
                return ('linux', '64')
            else:
                return ('linux', '32')
    elif current_system == "windows":
        if "arm" in current_machine:
            if "64" in current_machine:
                return ('windows', 'arm64', 'v8a')
            else:
                return ('windows', 'arm32', 'v7a')
        else:
            if "64" in current_machine:
                return ('windows', '64')
            else:
                return ('windows', '32')
    elif current_system == "darwin":
        if "arm" in current_machine:
            return ('macos', 'arm64', 'v8a')
        else:
            return ('macos', '64')
    # add other systems as needed
    else:
        raise OSError("Unsupported system: {current_system}")


def create_dir(dir_path):
    """creates a directory if it does not exist

    Args:
        dir_path (str): path to the directory to be created
    """
    if not os.path.exists(dir_path):
        os.makedirs(dir_path)
        
        
def get_n_lines(path: str) -> int:
    """returns the number of lines in a file

    Args:
        path (str): path to the file

    Returns:
        int: number of lines in the file
    """    
    with open(path, "rb") as infile:
        n_lines = 0
        for _, line in enumerate(infile):
            if line.strip():
                n_lines += 1        
    return n_lines
    
