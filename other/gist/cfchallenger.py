from __future__ import annotations

import argparse
import json
import logging
import re
from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional

import playwright.sync_api
from playwright._impl._api_types import Error as PlaywrightError
from playwright.sync_api import Frame, sync_playwright

Cookies = List[Dict[str, Any]]


class ChallengePlatform(Enum):
    """Cloudflare challenge platform URI paths."""

    JAVASCRIPT = "/cdn-cgi/challenge-platform/h/[bg]/orchestrate/jsch/v1"
    MANAGED = "/cdn-cgi/challenge-platform/h/[bg]/orchestrate/managed/v1"
    CAPTCHA = "/cdn-cgi/challenge-platform/h/[bg]/orchestrate/captcha/v1"


class CloudflareSolver:
    """
    A class for solving Cloudflare challenges with Playwright.

    Parameters
    ----------
    user_agent : str
        The user agent string to use for the browser requests.
    timeout : int
        The browser default timeout in seconds.
    http2 : bool
        Enable or disable the usage of HTTP/2 for the browser requests.
    http3 : bool
        Enable or disable the usage of HTTP/3 for the browser requests.
    headless : bool
        Enable or disable headless mode for the browser.
    proxy : Optional[str]
        The proxy server URL to use for the browser requests.

    Attributes
    ----------
    page : playwright.sync_api.Page
        The Playwright page.
    cookies : Cookies
        The cookies from current the page.

    Methods
    -------
    extract_clearance_cookie(cookies: Cookies) -> Optional[Dict[str, Any]]
        Extract the Cloudflare clearance cookie from a list of cookies.
    detect_challenge() -> Optional[ChallengePlatform]
        Detect the Cloudflare challenge platform on the current page.
    solve_challenge()
        Solve the Cloudflare challenge on the current page.
    """

    def __init__(
            self,
            *,
            user_agent: str,
            timeout: int,
            http2: bool,
            http3: bool,
            headless: bool,
            proxy: Optional[str],
    ) -> None:
        self._playwright = sync_playwright().start()

        if proxy is not None:
            proxy = self._parse_proxy(proxy)

        browser = self._playwright.firefox.launch(
            firefox_user_prefs={
                "network.http.http2.enabled": http2,
                "network.http.http3.enable": http3,
            },
            headless=headless,
            proxy=proxy,
        )

        context = browser.new_context()
        context.set_default_timeout(timeout * 1000)
        self.page = context.new_page()

    def __enter__(self) -> CloudflareSolver:
        return self

    def __exit__(self, *args: Any) -> None:
        self._playwright.stop()

    @staticmethod
    def _parse_proxy(proxy: str) -> Dict[str, str]:
        """
        Parse a proxy URL string into a dictionary of proxy parameters for the Playwright browser.

        Parameters
        ----------
        proxy : str
            Proxy URL string.

        Returns
        -------
        Dict[str, str]
            Dictionary of proxy parameters.
        """
        if "@" in proxy:
            proxy_regex = re.match("(.+)://(.+):(.+)@(.+)", proxy)
            server = f"{proxy_regex.group(1)}://{proxy_regex.group(4)}"

            proxy_params = {
                "server": server,
                "username": proxy_regex.group(2),
                "password": proxy_regex.group(3),
            }
        else:
            proxy_params = {"server": proxy}

        return proxy_params

    def _get_turnstile_frame(self) -> Optional[Frame]:
        """
        Get the Cloudflare turnstile frame.

        Returns
        -------
        Optional[Frame]
            Cloudflare turnstile frame.
        """
        for frame in self.page.frames:
            if (
                    re.match(
                        "https://challenges.cloudflare.com/cdn-cgi/challenge-platform/h/[bg]/turnstile",
                        frame.url,
                    )
                    is not None
            ):
                return frame

        return None

    @property
    def cookies(self) -> Cookies:
        """
        The cookies from the current page.

        Returns
        -------
        Cookies
            List of cookies.
        """
        return self.page.context.cookies()

    @staticmethod
    def extract_clearance_cookie(cookies: Cookies) -> Optional[Dict[str, Any]]:
        """
        Extract the Cloudflare clearance cookie from a list of cookies.

        Parameters
        ----------
        cookies : Cookies
            List of cookies.

        Returns
        -------
        Optional[Dict[str, Any]]
            cf_clearance cookie dictionary.
        """
        for cookie in cookies:
            if cookie["name"] == "cf_clearance":
                return cookie

        return None

    def detect_challenge(self) -> Optional[ChallengePlatform]:
        """
        Detect the Cloudflare challenge platform on the current page.

        Returns
        -------
        Optional[ChallengePlatform]
            Cloudflare challenge platform.
        """
        html = self.page.content()

        for platform in ChallengePlatform:
            if re.search(platform.value, html) is not None:
                return platform

        return None

    def solve_challenge(self) -> None:
        """Solve the Cloudflare challenge on the current page."""
        verify_button_pattern = re.compile(
            "Verify (I am|you are) (not a bot|(a )?human)"
        )

        verify_button = self.page.get_by_role("button", name=verify_button_pattern)
        challenge_spinner = self.page.locator("#challenge-spinner")
        challenge_stage = self.page.locator("div#challenge-stage")

        while (
                self.extract_clearance_cookie(self.cookies) is None
                and self.detect_challenge() is not None
        ):
            if challenge_spinner.is_visible():
                challenge_spinner.wait_for(state="hidden")

            if verify_button.is_visible():
                verify_button.click()
                challenge_stage.wait_for(state="hidden")
            elif self._get_turnstile_frame() is not None:
                turnstile_frame = self._get_turnstile_frame()
                turnstile_frame.get_by_role("checkbox").click()
                challenge_stage.wait_for(state="hidden")
            elif any(
                    frame.url.startswith("https://cf-assets.hcaptcha.com/captcha/v1")
                    for frame in self.page.frames
            ):
                self.page.reload()


def main() -> None:
    parser = argparse.ArgumentParser(
        description="A simple program for scraping Cloudflare clearance (cf_clearance) cookies from websites issuing Cloudflare challenges to visitors"
    )

    parser.add_argument(
        "url",
        metavar="URL",
        help="The URL to scrape the Cloudflare clearance cookie from",
        type=str,
    )

    parser.add_argument(
        "-f",
        "--file",
        default=None,
        help="The file to write the Cloudflare clearance cookie information to, in JSON format",
        type=str,
    )

    parser.add_argument(
        "-t",
        "--timeout",
        default=15,
        help="The browser default timeout in seconds",
        type=int,
    )

    parser.add_argument(
        "-p",
        "--proxy",
        default=None,
        help="The proxy server URL to use for the browser requests (SOCKS5 proxy authentication is not supported)",
        type=str,
    )

    parser.add_argument(
        "-ua",
        "--user-agent",
        # default= "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Chrome/112.0.5615.29",
        default="Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/109.0",
        help="The user agent to use for the browser requests",
        type=str,
    )

    parser.add_argument(
        "--disable-http2",
        action="store_true",
        help="Disable the usage of HTTP/2 for the browser requests",
    )

    parser.add_argument(
        "--disable-http3",
        action="store_true",
        help="Disable the usage of HTTP/3 for the browser requests",
    )

    parser.add_argument(
        "-d",
        "--debug",
        action="store_true",
        help="Run the browser in headed mode",
    )

    parser.add_argument(
        "-v",
        "--verbose",
        action="store_true",
        help="Increase the output verbosity",
    )

    args = parser.parse_args()
    logging_level = logging.INFO if args.verbose else logging.ERROR

    logging.basicConfig(
        format="[%(asctime)s] [%(levelname)s] %(message)s",
        datefmt="%H:%M:%S",
        level=logging_level,
    )

    logging.info("Launching %s browser...", "headed" if args.debug else "headless")

    challenge_messages = {
        ChallengePlatform.JAVASCRIPT: "Solving Cloudflare challenge [JavaScript]...",
        ChallengePlatform.MANAGED: "Solving Cloudflare challenge [Managed]...",
        ChallengePlatform.CAPTCHA: "Solving Cloudflare challenge [CAPTCHA]...",
    }

    with CloudflareSolver(
            user_agent=args.user_agent,
            timeout=args.timeout,
            http2=not args.disable_http2,
            http3=not args.disable_http3,
            headless=not args.debug,
            proxy=args.proxy,
    ) as solver:
        logging.info("Going to %s...", args.url)

        try:
            solver.page.goto(args.url)
        except PlaywrightError as err:
            logging.error(err)
            return

        challenge_platform = solver.detect_challenge()

        if challenge_platform is None:
            logging.error("No Cloudflare challenge detected.")
            return

        logging.info(challenge_messages[challenge_platform])

        try:
            solver.solve_challenge()
        except PlaywrightError as err:
            logging.error(err)

        clearance_cookie = solver.extract_clearance_cookie(solver.cookies)

        # loop = asyncio.get_event_loop()
        #
        # async def opser(page:playwright.sync_api.Page,address):
        #     # wait untill fully loaded
        #     print("waiting for "+address)
        #     page.wait_for_selector("[body]")
        #     # get the content
        #     print("Wait Finished for " + address)
        #     content = await page.content()
        #     # print the content
        #     print(content)
        # loop.run_until_complete(opser(solver.page,args.url))
        # loop.close()

        # await solver.page.wait_for_load_state(state="domcontentloaded")
        solver.page.wait_for_event("load")
        content = solver.page.content()
        print(content)

    if clearance_cookie is None:
        logging.error("Failed to retrieve the Cloudflare clearance cookie.")
        return

    if not args.verbose:
        print(clearance_cookie["value"])

    logging.info("Cookie: cf_clearance=%s", clearance_cookie["value"])
    logging.info("User agent: %s", args.user_agent)

    if args.file is None:
        return

    logging.info("Writing Cloudflare clearance cookie information to %s...", args.file)

    try:
        with open(args.file, encoding="utf-8") as file:
            json_data = json.load(file)
    except (FileNotFoundError, json.JSONDecodeError):
        json_data = {"clearance_cookies": []}

    # Get the unix timestamp using the cookie's expiration date minus one year
    unix_timestamp = clearance_cookie["expires"] - 31557600
    timestamp = datetime.utcfromtimestamp(unix_timestamp).isoformat()

    json_data["clearance_cookies"].append(
        {
            "unix_timestamp": unix_timestamp,
            "timestamp": timestamp,
            "domain": clearance_cookie["domain"],
            "cf_clearance": clearance_cookie["value"],
            "user_agent": args.user_agent,
            "proxy": args.proxy,
        }
    )

    with open(args.file, "w", encoding="utf-8") as file:
        json.dump(json_data, file, indent=4)


if __name__ == "__main__":
    main()
