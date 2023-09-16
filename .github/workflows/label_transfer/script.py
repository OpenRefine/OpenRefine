import requests
import os
from lxml import html
import sys
import json

# Config

# list of labels that should not be transferred to PRs
do_not_transfer = [
    'good first issue',
    'good second issue',
    'imported from old code repo',
    'help wanted',
    'duplicate',
    'invalid',
    'question',
    'to be reviewed',
]

# Internal

repo = os.environ.get('GITHUB_REPO')
github_token = os.environ.get('GITHUB_TOKEN')

headers = {
    'Accept': 'application/vnd.github.v3+json',
}

if github_token:
    headers['Authorization'] = 'Bearer '+github_token

def get_linked_issues(pr_number):
    """
    Given a PR number, extract all the linked issue numbers from it.
    Sadly this is not supported by the API yet, so we just scrape the web UI.
    """
    url = f'https://github.com/{repo}/pull/{pr_number}'
    page = requests.get(url)
    page.raise_for_status()
    parsed = html.document_fromstring(page.text)
    matches = parsed.xpath('//form/span[@class="Truncate truncate-with-responsive-width my-1"]/a')
    for match in matches:
        yield int(match.attrib['href'].split('/')[-1])

def get_issue_labels(issue_number):
    """
    Returns all the labels in a given issue / PR
    """
    url = f'https://api.github.com/repos/{repo}/issues/{issue_number}/labels'
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    return [ tag['name'] for tag in response.json() ]

def transfer_issue_labels(pr_number):
    """
    Transfers labels from all the linked issues to the PR
    """
    linked_issues = get_linked_issues(pr_number)
    if not linked_issues:
        print('No linked issues found')
    all_labels = [ label for issue in linked_issues for label in get_issue_labels(issue) ]
    to_transfer = [ label for label in all_labels if label not in do_not_transfer ]
    current_labels = get_issue_labels(pr_number)
    missing_labels = [ label for label in to_transfer if label not in current_labels ]
    if not missing_labels:
        return
    new_labels = current_labels + missing_labels
    url = f'https://api.github.com/repos/{repo}/issues/{pr_number}/labels'
    print(f'adding {missing_labels} to PR #{pr_number}')
    if not github_token:
        print('no GITHUB_TOKEN, skipping')
    else:
        resp = requests.put(url, headers=headers, data=json.dumps({'labels':new_labels}))
        resp.raise_for_status()

if __name__ == '__main__':
    transfer_issue_labels(sys.argv[1])
