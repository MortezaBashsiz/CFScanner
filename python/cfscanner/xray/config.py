import os

from ..args.testconfig import TestConfig
from ..utils.socket import get_free_port
import uuid


def create_proxy_config(
    edge_ip,
    test_config: TestConfig,
    config_dir: str,
) -> str:
    """creates proxy (v2ray/xray) config json file based on ``TestConfig`` instance

    Args:
        edge_ip (str): Cloudflare edge ip to use for the config
        test_config (TestConfig): instance of ``TestConfig`` containing information about the setting of the test
        config_dir (str): the directory to save the config file to 

    Returns:
        config_path (str): the path to the json file created
    """
    test_config.local_port = get_free_port()
    local_port_str = str(test_config.local_port)
    config = test_config.proxy_config_template.replace(
        "PORTPORT", local_port_str)
    config = config.replace("IP.IP.IP.IP", edge_ip)
    
    if not test_config.custom_template and (not test_config.novpn):
        config = config.replace("CFPORTCFPORT", str(test_config.address_port))
        config = config.replace("IDID", test_config.user_id)
        config = config.replace("HOSTHOST", test_config.ws_header_host)
        config = config.replace("ENDPOINTENDPOINT", test_config.ws_header_path)
        hostname = test_config.ws_header_host.split(".", maxsplit=1)[1]
        random_sni = f"{uuid.uuid4()}.{hostname}"
        config = config.replace("RANDOMHOST", random_sni)

    config_path = os.path.join(config_dir, f"config-{edge_ip.replace(':', '_')}.json")
    with open(config_path, "w") as configFile:
        configFile.write(config)

    return config_path
