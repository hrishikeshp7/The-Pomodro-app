#!/bin/bash
echo "Finding potential performance issues..."
grep -r "@Composable" app/ | wc -l
grep -r "collectAsState" app/
grep -r "collectAsStateWithLifecycle" app/
grep -r "remember" app/
