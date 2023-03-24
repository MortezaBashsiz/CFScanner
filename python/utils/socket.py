import socket
import socketserver
import time


def get_free_port() -> int:
    """returns a free port

    Returns:
        int: free port
    """
    with socketserver.TCPServer(("localhost", 0), None) as s:
        free_port = s.server_address[1]
    return free_port


def wait_for_port(
    port: int,
    host: str = 'localhost',
    timeout: float = 5.0
) -> None:
    """Wait until a port starts accepting TCP connections.
    Args:
        port: Port number.
        host: Host address on which the port should exist.
        timeout: In seconds. How long to wait before raising errors.
    Raises:
        TimeoutError: The port isn't accepting connection after time specified in `timeout`.
    """
    start_time = time.perf_counter()
    while True:
        try:
            with socket.create_connection((host, port), timeout=timeout):
                break
        except OSError as ex:
            time.sleep(0.01)
            if time.perf_counter() - start_time >= timeout:
                raise TimeoutError(
                    f'Timeout exceeded for the port {port} on host {host} to start accepting connections.'
                ) from ex
