def save_results(results: list, save_path: str, sort=True):
    """saves results to file

    Args:
        results (list): a list of (ms, ip) tuples
        save_path (str): the path to save the file
        sort (bool, optional): binary indicating if the results should be
        sorted based on the response time Defaults to True.
    """
    # clean the results and make sure the first element is integer
    results = [(int(float(result[0])), result[1]) for result in results]

    if sort:
        results.sort(key=lambda r: r[0])

    with open(save_path, "w") as outfile:
        outfile.write("\n".join([" ".join(map(str, res)) for res in results]))
        outfile.write("\n")
