def make_permutations_and_neighbors(n):
    index = {}
    perms = []

    p = range(n)

    while (1):
        index[tuple(p)] = len(perms)
        perms.append(tuple(p))

        i = n-2
        while i >= 0 and p[i] >= p[i+1]:
            i = i - 1
        if i < 0:
            break

        j = n-1
        while p[j] <= p[i]:
            j = j - 1
        p[i], p[j] = p[j], p[i]
        i = i + 1
        j = n-1
        while i < j:
            p[i], p[j] = p[j], p[i]
            i, j = i+1, j-1

    neighbors = [None] * len(perms)
    for i in range(len(perms)):
        p = perms[i]
        row = neighbors[i] = [0] * (n-1)
        for j in range(n-1):
            q = list(p)
            q[j], q[j+1] = q[j+1], q[j]
            row[j] = index[tuple(q)]

    return perms, neighbors


def apply_perm(p, i):
    if 0 <= i < len(p):
        return p[i]
    else:
        return i


def barycentric_subdivision(ds, split_dim):
    if not 0 <= split_dim <= ds.dim():
        raise ValueError("split_dim must be between 0 and ds.dim()")

    ds = ds.straight()
    if split_dim == 0:
        return ds

    dim = ds.dim()

    (perms, neighbors) = make_permutations_and_neighbors(split_dim+1)
    m = len(perms)
    newsize = ds.size() * m

    result = DSymbol(dim = dim, size = newsize)

    base = 1
    for D in ds.elements():
        for j in range(m):
            p = perms[j]
            for i in range(split_dim):
                k = neighbors[j][i]
                result.set_op(i, base + j, base + k)
            for i in range(split_dim, dim+1):
                E = ds.op(apply_perm(p, i), D)
                result.set_op(i, base + j, (E-1) * m + j + 1)
        base = base + m

    base = 1
    for D in ds.elements():
        for j in range(m):
            p = perms[j]
            for i in range(dim):
                if i < split_dim-1:
                    v = 1
                else:
                    v = ds.v(apply_perm(p, i), apply_perm(p, i+1), D)
                result.set_v(i, i+1, base + j, v)
        base = base + m
    
    return result
