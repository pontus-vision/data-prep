function RemoteModel(options) {
	const {
		pageSize = 50,
		data = { length: 0 },
		getData,
		idKey,
	} = options;

	let reqTimer;
	let filters = [];
	const indexById = {};
	const onDataLoading = new Slick.Event();
	const onDataLoaded = new Slick.Event();

	function clear() {
		Object.keys(data).forEach(key => delete data[key]);
		Object.keys(indexById).forEach(key => delete indexById[key]);
		data.length = 0;
	}

	function clearFilters() {
		filters = [];
	}

	function isDataLoaded(from, to) {
		for (let i = from; i <= to; i++) {
			if (data[i] === undefined || data[i] === null) {
				return false;
			}
		}

		return true;
	}

	function ensureData(from, to) {
		if (from < 0) {
			from = 0;
		}

		if (data.length > 0) {
			to = Math.min(to, data.length - 1);
		}

		let fromPage = Math.floor(from / pageSize);
		let toPage = Math.floor(to / pageSize);

		while (data[fromPage * pageSize] !== undefined && fromPage < toPage) {
			fromPage++;
		}

		while (data[toPage * pageSize] !== undefined && fromPage < toPage) {
			toPage--;
		}

		if (fromPage > toPage || ((fromPage === toPage) && data[fromPage * pageSize] !== undefined)) {
			onDataLoaded.notify({ from, to });
			return;
		}

		if (reqTimer) {
			clearTimeout(reqTimer);
		}
		reqTimer = setTimeout(() => {
			for (let i = fromPage; i <= toPage; i++) {
				data[i * pageSize] = null; // null indicates a 'requested but not available yet'
			}
			getData({ pageSize, fromPage, toPage, filters })
				.then(resp => onSuccess(resp, fromPage * pageSize))
				.then(() => onDataLoaded.notify({ from, to }))
				.catch((error) => {
					console.error(error);
					// reset the data to mark them as unavailable
					for (let i = fromPage; i <= toPage; i++) {
						data[i * pageSize] = undefined;
					}
				});
		}, 100);
	}

	function onSuccess(resp, from) {
		for (let i = 0; i < resp.length; i++) {
			const item = resp[i];
			const index = from + i;
			data[index] = item;
			indexById[item[idKey]] = index;
		}
	}

	function reloadData(from, to) {
		for (let i = from; i <= to; i++) {
			delete data[i];
		}

		ensureData(from, to);
	}

	function setFilters(newFilters) {
		clear();
		clearFilters();
		filters = newFilters;
		ensureData(0, pageSize);
	}

	function setLength(length) {
		data.length = length;
	}

	function getIndexByItem(item) {
		return indexById[item[idKey]];
	}

	function getItemByIndex(index) {
		return data[index];
	}

	return {
		// properties
		data,

		// methods
		clear,
		ensureData,
		getIndexByItem,
		getItemByIndex,
		isDataLoaded,
		reloadData,
		setFilters,
		setLength,

		// events
		onDataLoading,
		onDataLoaded,
	};
}

export default RemoteModel;
